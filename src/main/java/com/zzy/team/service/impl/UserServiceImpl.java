package com.zzy.team.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.zzy.team.constant.ErrorStatus;
import com.zzy.team.constant.UserConstant;
import com.zzy.team.exception.BusinessException;
import com.zzy.team.mapper.UserMapper;
import com.zzy.team.model.domain.User;
import com.zzy.team.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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

    private final static String SALT = "1a2b3c4d5e";

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
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
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        // 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        boolean save = this.save(user);
        if (!save) {
            throw new BusinessException(ErrorStatus.SERVICE_ERROR, "注册失败");
        }
        // Long拆箱到long，如果Long为null，会拆箱错误
        return user.getId();
    }

    @Override
    public User doLogin(String userAccount, String userPassword, HttpServletRequest httpRequest) {
        // 1.校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return null;
        }
        // 用户名长度
        if (userAccount.length() < 4) {
            return null;
        }
        // 密码长度
        if (userPassword.length() < 8) {
            return null;
        }
        // 账号不能包含特殊字符, 只允许字母、数字和下划线
        String validatePattern = "^[a-zA-Z0-9_]+$";
        Matcher matcher = Pattern.compile(validatePattern).matcher(userAccount);
        if (!matcher.matches()) {
            // 包含了特殊字符，没有匹配到返回-1
            return null;
        }

        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.getOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            return null;
        }
        // 数据脱敏
        User safeUser = getSafeUser(user);
        // 记录用户的登陆状态，此处目前是单机的方式
        httpRequest.getSession().setAttribute(UserConstant.SING_KEY, safeUser);

        return safeUser;
    }

    @Override
    public int userLogout(HttpServletRequest httpServletRequest) {
        // 移除登陆状态
        httpServletRequest.getSession().removeAttribute(UserConstant.SING_KEY);
        return 1;
    }

    /**
     * 用户脱敏
     *
     * @param originalUse
     * @return
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
            for (String tag : set) {
                if (!set.contains(tag)) {
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList());
        return users;
    }
}




