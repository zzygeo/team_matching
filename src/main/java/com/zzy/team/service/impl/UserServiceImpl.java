package com.zzy.team.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.zzy.team.constant.ErrorStatus;
import com.zzy.team.constant.UserConstant;
import com.zzy.team.exception.BusinessException;
import com.zzy.team.model.domain.User;
import com.zzy.team.service.UserService;
import com.zzy.team.service.mapper.UserMapper;
import com.zzy.team.utils.StringCompareUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author zzy
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2024-05-07 13:20:58
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {
    @Autowired
    RedisTemplate redisTemplate;

    @Override
    public boolean userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1.校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "用户名或密码不能为空");
        }
        // 用户名长度
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "用户名长度不能小于4位");
        }
        // 密码长度
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "密码长度不能小于8位");
        }
        // 账号不能包含特殊字符, 只允许字母、数字和下划线
        String validatePattern = "^[a-zA-Z0-9_]+$";
        Matcher matcher = Pattern.compile(validatePattern).matcher(userAccount);
        if (!matcher.matches()) {
            // 包含了特殊字符，没有匹配到返回-1
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "用户名只能包含字母、数字和下划线");
        }
        // 不一致
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 用户名不能重复, 放在最后，减少数据库的查询
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_account", userAccount);
        long count = this.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "账号重复");
        }
        // 密码加密
        String encryptPassword = DigestUtils.md5DigestAsHex((UserConstant.PASSWORD_SALT + userPassword).getBytes());

        // 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        boolean save = this.save(user);
        if (!save) {
            throw new BusinessException(ErrorStatus.SERVICE_ERROR, "注册失败");
        }
        // Long拆箱到long，如果Long为null，会拆箱错误
        return true;
    }

    @Override
    public User doLogin(String userAccount, String userPassword, HttpServletRequest httpRequest) {
        // 1.校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "用户名或密码不能为空");
        }
        // 用户名长度
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "用户名长度不能小于4位");
        }
        // 密码长度
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "密码长度不能小于8位");
        }
        // 账号不能包含特殊字符, 只允许字母、数字和下划线
        String validatePattern = "^[a-zA-Z0-9_]+$";
        Matcher matcher = Pattern.compile(validatePattern).matcher(userAccount);
        if (!matcher.matches()) {
            // 包含了特殊字符，没有匹配到返回-1
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "用户名只能包含字母、数字和下划线");
        }

        String encryptPassword = DigestUtils.md5DigestAsHex((UserConstant.PASSWORD_SALT + userPassword).getBytes());
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_account", userAccount);
        queryWrapper.eq("user_password", encryptPassword);
        User user = this.getOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "用户名或密码不正确");
        }
        // 数据脱敏
        User safeUser = getSafeUser(user);
        // 记录用户的登陆状态，此处目前是单机的方式
        httpRequest.getSession().setAttribute(UserConstant.SING_KEY, safeUser);

        return safeUser;
    }

    @Override
    public void userLogout(HttpServletRequest httpServletRequest) {
        // 移除登陆状态
        httpServletRequest.getSession().removeAttribute(UserConstant.SING_KEY);
    }

    @Override
    public boolean updateUser(User user, User loginUser) {
        Long id = user.getId();
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "用户id不能为空");
        }
        // 判断权限，仅管理员和自己可以修改信息
        if (!isAdmin(loginUser) || !user.getId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "用户权限不足");
        }
        User byId = this.getById(id);
        // 先判断用户存不存在
        if (byId == null) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "用户不存在");
        }
        // todo 如果什么都没传的话，那么就不更新

        return this.updateById(user);
    }

    /**
     * 用户脱敏
     *
     * @return
     * @par User loginUser = UserHolder.getUser();am originalUse
     */
    public User getSafeUser(User originalUse) {
        if (originalUse == null) {
            return null;
        }
        User safeUser = new User();
        safeUser.setId(originalUse.getId());
        safeUser.setUsername(originalUse.getUsername());
        safeUser.setUserAccount(originalUse.getUserAccount());
        safeUser.setAvatarUrl(originalUse.getAvatarUrl());
        safeUser.setGender(originalUse.getGender());
        safeUser.setPhone(originalUse.getPhone());
        safeUser.setEmail(originalUse.getEmail());
        safeUser.setUserStatus(originalUse.getUserStatus());
        safeUser.setCreateTime(originalUse.getCreateTime());
        safeUser.setUserRole(originalUse.getUserRole());
        safeUser.setTags(originalUse.getTags());
        safeUser.setPlanetCode(originalUse.getPlanetCode());
        return safeUser;
    }

    /**
     * 根据用户标签搜索用户
     *
     * @param tags
     * @return
     */
    @Override
    public List<User> searchUserByTagsSql(List<String> tags) {
        // 为空或者长度为0
        if (CollectionUtils.isEmpty(tags)) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR);
        }
        // sql层面查询
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        for (String tag : tags) {
            // 拼接多个 like
            queryWrapper = queryWrapper.like("tags", tag);
        }
        List<User> userList = this.list(queryWrapper);
        return userList.stream().map(this::getSafeUser).collect(Collectors.toList());
    }

    @Override
    public List<User> searchUserByMemory(List<String> tags) {
        // 为空或者长度为0
        if (CollectionUtils.isEmpty(tags)) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR);
        }
        // 在内存中查询
        Gson gson = new Gson();
        List<User> list = this.list();
        List<User> users = list.stream().filter((user) -> {
            String userTags = user.getTags();
            if (userTags == null) {
                return false;
            }
            Set<String> set = gson.fromJson(userTags, Set.class);
            for (String tag : tags) {
                if (!set.contains(tag)) {
                    return false;
                }
            }
            return true;
        }).map(this::getSafeUser).collect(Collectors.toList());
        return users;

    }

    @Override
    public Page<User> pageUsers(Integer pageNum, Integer pageSize, User loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorStatus.UNAUTHORIZED_ERROR, "用户未登陆");
        }
        // 根据当前登陆的用户推荐伙伴，推荐第一页
        String user_key = UserConstant.RECOMMEND_USER + loginUser.getId() + ":" + pageNum;
        Page<User> redisUserPage = (Page<User>) redisTemplate.opsForValue().get(user_key);
        if (redisUserPage != null) {
            return redisUserPage;

        }
        Page<User> userPage = new Page<>(pageNum, pageSize);
        Page<User> page = this.page(userPage);
        List<User> users = page.getRecords().stream().map(this::getSafeUser).collect(Collectors.toList());
        page.setRecords(users);
        try {
            redisTemplate.opsForValue().set(user_key, page, 20000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("redis key set error: ", e);
        }
        return page;
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && user.getUserRole().equals(UserConstant.ADMIN_ROLE);
    }

    @Override
    public List<User> matchUser(Integer max, User loginUser) {
        if (max == null || max < 1 || max > 20) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR);
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(User::getTags);
        wrapper.select(User::getId, User::getTags);
        wrapper.ne(User::getTags, "[]");
        // 假如我先查询所有的数据
        List<User> list = this.list(wrapper);
        // 创建一个排序的map
        List<Pair<User, Integer>> pairs = new ArrayList<>();
        for (User user : list) {
            String userTags = user.getTags();
            ObjectMapper objectMapper = new ObjectMapper();
            List<String> queryTags;
            List<String> loginUserTags;
            try {
                queryTags = objectMapper.readValue(userTags, new TypeReference<>() {
                });
                loginUserTags = objectMapper.readValue(loginUser.getTags(), new TypeReference<>() {
                });
            } catch (JsonProcessingException e) {
                // 遇到异常直接跳过不处理当前条
                continue;
            }
            // 剔除掉自己，包装类不要用 == 判断
            if (!user.getId().equals(loginUser.getId())) {
                int i = StringCompareUtil.similarRates(queryTags, loginUserTags);
                // 用户多了有内存增加的风险
                pairs.add(new Pair<>(user, i));
            }
        }
        List<Pair<User, Integer>> collect = pairs.stream().sorted(Comparator.comparing(Pair::getValue)).collect(Collectors.toList());
        List<Long> userIds = collect.stream().map(p -> p.getKey().getId()).limit(max).collect(Collectors.toList());
        List<User> users = this.listByIds(userIds).stream().map(this::getSafeUser).sorted(new Comparator<User>() {
            @Override
            public int compare(User o1, User o2) {
                int i = userIds.indexOf(o1.getId());
                int i2 = userIds.indexOf(o2.getId());
                return Integer.compare(i, i2);
            }
        }).collect(Collectors.toList());
        return users;
    }

    @Override
    public User getSafeUserInfo(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR);
        }
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "要查询的id不存在");
        }
        user.setPhone(null);
        user.setEmail(null);
        user.setUserPassword(null);
        return user;
    }
}




