package com.zzy.team.model.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

@Data
public class UserVo {

    private Long id;

    /**
     * 用户昵称
     */
    private String username;

    /**
     * 用户登录账号
     */
    private String userAccount;

    /**
     * 个人简介
     */
    private String profile;

    /**
     * 用户头像
     */
    private String avatarUrl;

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
}
