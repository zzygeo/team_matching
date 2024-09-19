package com.zzy.team.model.domain;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 用户表
 * @TableName user
 */
@TableName(value ="user")
@Data
@Accessors(chain = true)
public class User implements Serializable {
    /**
     * 用户id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户昵称
     */
    @TableField(value = "username")
    private String username;

    /**
     * 用户登录账号
     */
    @TableField(value = "user_account")
    private String userAccount;

    /**
     * 个人简介
     */
    @TableField(value = "profile")
    private String profile;

    /**
     * 用户头像
     */
    @TableField(value = "avatar_url")
    private String avatarUrl;

    /**
     * 用户密码
     */
    @TableField(value = "user_password")
    private String userPassword;

    /**
     * 用户手机号
     */
    @TableField(value = "phone")
    private String phone;

    /**
     * 用户邮箱
     */
    @TableField(value = "email")
    private String email;

    /**
     * 用户状态 0 正常 1 禁用
     */
    @TableField(value = "user_status")
    private Integer userStatus;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private Date updateTime;

    /**
     * 0表示未删除，1表示删除
     */
    @TableField(value = "is_delete")
    @TableLogic
    private Integer isDelete;

    /**
     * 用户状态 0 普通用户 1 管理员
     */
    @TableField(value = "user_role")
    private Integer userRole;

    /**
     * 星球编号
     */
    @TableField(value = "planet_code")
    private String planetCode;

    /**
     * 用户标签
     */
    @TableField(value = "tags")
    private String tags;

    /**
     * 性别 0-未知 1-男 2-女
     */
    @TableField(value = "gender")
    private Integer gender;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}