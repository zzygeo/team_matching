package com.zzy.team.utils;

import com.zzy.team.constant.ErrorStatus;
import com.zzy.team.constant.UserConstant;
import com.zzy.team.exception.BusinessException;
import com.zzy.team.model.domain.User;

import javax.servlet.http.HttpServletRequest;

public class ServletUtils {
    public static User getLoginUser(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        User user = (User) request.getSession().getAttribute(UserConstant.SING_KEY);
        if (user == null) {
            throw new BusinessException(ErrorStatus.UNAUTHORIZED_ERROR, "未登录");
        }
        return user;
    }
}
