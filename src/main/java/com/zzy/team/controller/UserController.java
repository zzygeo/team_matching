package com.zzy.team.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zzy.team.common.Result;
import com.zzy.team.constant.ErrorStatus;
import com.zzy.team.constant.UserConstant;
import com.zzy.team.exception.BusinessException;
import com.zzy.team.model.domain.User;
import com.zzy.team.model.request.UserLoginRequest;
import com.zzy.team.model.request.UserRegisterRequest;
import com.zzy.team.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Long userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
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
        return userService.userRegister(userAccount, userPassword, checkPassword);
    }

    @PostMapping("/login")
    public Result userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest httpServletRequest) {
        if (userLoginRequest == null) {

            return null;
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return null;
        }
        User user = userService.doLogin(userAccount, userPassword, httpServletRequest);
        return Result.OK(user);
    }

    @PostMapping("/logout")
    public Integer logout(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return userService.userLogout(request);
    }

    @GetMapping("/current")
    public User getCurrentUser(HttpServletRequest httpServletRequest) {
        User user = (User) httpServletRequest.getSession().getAttribute(UserConstant.SING_KEY);
        if (user == null) {
            return null;
        }
        // cookie里的信息可能不是最新的，可以再进行一遍查询
        Long id = user.getId();
        User userById = userService.getById(id);
        // todo 校验用户是否合法
        return userService.getSafeUser(userById);
    }

    @GetMapping("/search")
    public List<User> searchUsers(String username, HttpServletRequest httpServletRequest) {
        boolean admin = isAdmin(httpServletRequest);
        if (!admin) {
            return null;
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like("username", username);
        }
        List<User> list = userService.list(queryWrapper);
        return list.stream().map(user -> userService.getSafeUser(user)).collect(Collectors.toList());
    }

    @PostMapping("/delete")
    public boolean deleteUser(long id, HttpServletRequest httpServletRequest) {
        // 仅管理员可查询
        boolean admin = isAdmin(httpServletRequest);
        if (!admin) {
            return false;
        }
        if (id <= 0) {
            return false;
        }
        return userService.removeById(id);
    }

    /**
     *  用户是否是管理员
     * @param httpServletRequest
     * @return
     */
    private boolean isAdmin(HttpServletRequest httpServletRequest) {
        User user = (User) httpServletRequest.getSession().getAttribute(UserConstant.SING_KEY);

        if (user == null || user.getUserRole() != UserConstant.ADMIN_ROLE) {
            // 用户无权限
            return false;
        }
        return true;
    }
}
