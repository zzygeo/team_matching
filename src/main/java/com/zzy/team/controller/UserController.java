package com.zzy.team.controller;

import cn.hutool.bloomfilter.BitMapBloomFilter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzy.team.common.Result;
import com.zzy.team.constant.ErrorStatus;
import com.zzy.team.constant.UserConstant;
import com.zzy.team.exception.BusinessException;
import com.zzy.team.manager.UserRecommendationManager;
import com.zzy.team.model.domain.Team;
import com.zzy.team.model.domain.User;
import com.zzy.team.model.request.UserLoginRequest;
import com.zzy.team.model.request.UserRegisterRequest;
import com.zzy.team.service.UserService;
import com.zzy.team.utils.ServletUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user")
@Slf4j
@Api(tags = {"用户操作接口"})
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    RedisTemplate redisTemplate;

    @PostMapping("/register")
    @ApiOperation("用户注册")
    public Result userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();

        // controller层一般对本身的数据的校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR);
        }
        boolean b = userService.userRegister(userRegisterRequest);
        return b ? Result.OK("注册成功", null) : Result.FAIL(ErrorStatus.SERVICE_ERROR, "注册失败");
    }

    @PostMapping("/login")
    @ApiOperation("用户登录")
    public Result userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest httpServletRequest) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "用户名或密码不能为空");
        }
        User user = userService.doLogin(userAccount, userPassword, httpServletRequest);
        return Result.OK(user);
    }

    @PostMapping("/logout")
    @ApiOperation("用户登出")
    public Result logout(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        userService.userLogout(request);
        return Result.OK("退出成功", null);
    }

    @GetMapping("/current")
    @ApiOperation("获取当前登录用户信息")
    public Result getCurrentUser(HttpServletRequest httpServletRequest) {
        User user = (User) httpServletRequest.getSession().getAttribute(UserConstant.SING_KEY);
        if (user == null) {
            throw new BusinessException(ErrorStatus.UNAUTHORIZED_ERROR, "用户未登录");
        }
        // cookie里的信息可能不是最新的，可以再进行一遍查询
        Long id = user.getId();
        User userById = userService.getById(id);
        // todo 校验用户是否合法
        return Result.OK(userService.getSafeUser(userById));
    }

    @GetMapping("/search")
    @ApiOperation("搜索用户")
    public Result searchUsers(String username, HttpServletRequest httpServletRequest) {
        User loginUser = ServletUtils.getLoginUser(httpServletRequest);
        boolean admin = userService.isAdmin(loginUser);
        if (!admin) {
            throw new BusinessException(ErrorStatus.FORBIDDEN_ERROR, "无权限");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like("username", username);
        }
        List<User> list = userService.list(queryWrapper);
        return Result.OK(list.stream().map(user -> userService.getSafeUser(user)).collect(Collectors.toList()));
    }

    @GetMapping("/recommend")
    @ApiOperation("推荐用户")
    public Result recommendUsers(HttpServletRequest request) {
        // 先获取过滤器，如果没有就创建
        // 随机推送数据库里的用户，如果有重复的，就丢弃，直到推荐到10个为止
        User loginUser = ServletUtils.getLoginUser(request);
        Long id = loginUser.getId();
        BitMapBloomFilter bitMapBloomFilter = UserRecommendationManager.get(id);
        if (bitMapBloomFilter == null) {
            bitMapBloomFilter = UserRecommendationManager.create(id);
        }
        int total = 0;
        List<User> lists = new ArrayList<>();
        while (total < 10) {
            User user = userService.recomendUser();
            boolean contains = bitMapBloomFilter.contains(user.getId().toString());
            if (!contains) {
                lists.add(user);
                total++;
            }
        }
        return Result.OK(lists);
    }

    @PostMapping("/delete")
    @ApiOperation("删除用户")
    public Result deleteUser(long id, HttpServletRequest httpServletRequest) {
        User loginUser = ServletUtils.getLoginUser(httpServletRequest);
        // 仅管理员可查询
        boolean admin = userService.isAdmin(loginUser);
        if (!admin) {
            throw new BusinessException(ErrorStatus.FORBIDDEN_ERROR, "无权限");
        }
        if (id <= 0) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "用户id不合法");
        }
        boolean b = userService.removeById(id);
        return b ? Result.OK("删除成功", null) : Result.FAIL(ErrorStatus.SERVICE_ERROR, "删除失败");
    }

    @GetMapping("/search/tags")
    @ApiOperation("通过标签搜索用户")
    public Result searchUsersByTags(@RequestParam(required = false) List<String> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "标签不能为空");
        }
        List<User> users = userService.searchUserByTagsSql(tags);
        return Result.OK(users);
    }

    @PostMapping("/update")
    @ApiOperation("更新用户信息")
    public Result updateUser(@RequestBody User user, HttpServletRequest request) {
        if (user == null) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "用户不能为空");
        }

        User loginUser = ServletUtils.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorStatus.UNAUTHORIZED_ERROR, "用户未登录");
        }
        boolean b = userService.updateUser(user, loginUser);
        return b ? Result.OK("更新成功", null) : Result.FAIL(ErrorStatus.SERVICE_ERROR, "更新失败");
    }

    @GetMapping("/match")
    @ApiOperation("用户匹配")
    Result<List<User>> match(Integer max, HttpServletRequest request) {
        if (max == null || max < 1 || max > 20) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "最大匹配人数不合法");
        }
        User loginUser = ServletUtils.getLoginUser(request);
        List<User> user =  userService.matchUser(max, loginUser);
        return Result.OK(user);
    }

    @GetMapping("/getSafeUserInfo")
    @ApiOperation("获取安全用户信息")
    Result<User> getSafeUserInfo(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "用户id不合法");
        }
        User user = userService.getSafeUserInfo(userId);
        return Result.OK(user);
    }
}
