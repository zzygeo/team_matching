package com.zzy.team.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
     *
     * @param userAccount   用户名
     * @param userPassword  用户密码
     * @param checkPassword 确认密码
     * @return 注册成功-返回用户id，注册失败-抛出异常
     */
    boolean userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     * @param userAccount 用户名
     * @param userPassword 密码
     * @return
     */
    User doLogin(String userAccount, String userPassword, HttpServletRequest httpRequest);

    /**
     * 用户注销
     *
     * @param httpServletRequest
     */
    void userLogout(HttpServletRequest httpServletRequest);

    /**
     * 更新用户
     * @param user
     * @param loginUser
     * @return
     */
    boolean updateUser(User user, User loginUser);

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

    /**
     * 根据标签搜索用户，使用内存搜索
     * @param tags
     * @return
     */
    List<User> searchUserByMemory(List<String> tags);

    /**
     * 分页查询用户
     * @param pageNum
     * @param pageSize
     * @return
     */
    Page<User> pageUsers(Integer pageNum, Integer pageSize, User loginUser);

    boolean isAdmin(User user);
}
