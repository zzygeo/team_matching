package com.zzy.team.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzy.team.model.domain.User;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author zzy
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2024-05-07 13:20:58
*/
public interface UserService extends IService<User> {
    /**
     * 用户注册
     * @param userAccount 用户名
     * @param userPassword 用户密码
     * @param checkPassword 确认密码
     * @return 注册成功-返回用户id，注册失败-抛出异常
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     * @param userAccount 用户名
     * @param userPassword 密码
     * @return
     */
    User doLogin(String userAccount, String userPassword, HttpServletRequest httpRequest);

    /**
     * 用户注销
     * @param httpServletRequest
     * @return
     */
    int userLogout(HttpServletRequest httpServletRequest);

    /**
     * 用户脱敏
     * @param originalUse
     * @return
     */
    User getSafeUser(User originalUse);

    /**
     * 根据标签搜索用户
     *
     * @param tags
     * @return
     */
    List<User> searchUserByTagsSql(List<String> tags);

    List<User> searchUserByMemory(List<String> tags);
}
