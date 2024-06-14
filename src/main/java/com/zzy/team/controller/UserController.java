package com.zzy.team.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzy.team.common.Result;
import com.zzy.team.constant.ErrorStatus;
import com.zzy.team.constant.UserConstant;
import com.zzy.team.exception.BusinessException;
import com.zzy.team.model.domain.User;
import com.zzy.team.model.request.UserLoginRequest;
import com.zzy.team.model.request.UserRegisterRequest;
import com.zzy.team.service.UserService;
import com.zzy.team.utils.ServletUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user")
@CrossOrigin(value = {"http://localhost:5173"}, allowCredentials = "true")
@Slf4j
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    RedisTemplate redisTemplate;

    @PostMapping("/register")
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
        boolean b = userService.userRegister(userAccount, userPassword, checkPassword);
        return b ? Result.OK("注册成功", null) : Result.FAIL(ErrorStatus.SERVICE_ERROR, "注册失败");
    }

    @PostMapping("/login")
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
    public Result logout(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        userService.userLogout(request);
        return Result.OK("退出成功", null);
    }

    @GetMapping("/current")
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
    public Result recommendUsers(Integer pageNum, Integer pageSize, HttpServletRequest request) {
        // 先从缓存中读取数据
        if (pageNum == null || pageNum < 1 || pageSize == null || pageSize < 1) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "页码或页数不合法");
        }
        User loginUser = ServletUtils.getLoginUser(request);
        Page<User> userPage = userService.pageUsers(pageNum, pageSize, loginUser);
        return Result.OK(userPage);
    }

    @PostMapping("/delete")
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
    public Result searchUsersByTags(@RequestParam(required = false) List<String> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            throw new BusinessException(ErrorStatus.PARAMS_ERROR, "标签不能为空");
        }
        List<User> users = userService.searchUserByTagsSql(tags);
        return Result.OK(users);
    }

    @PostMapping("/update")
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

}
