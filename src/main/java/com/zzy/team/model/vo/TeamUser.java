package com.zzy.team.model.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zzy.team.model.domain.User;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 队伍和用户信息封装类
 */
@Data
public class TeamUser {
    /**
     * id
     */
    private Long id;

    /**
     * 队名
     */
    private String teamName;

    /**
     * 描述
     */
    private String description;

    /**
     * 登陆的用户是否已经加入队伍
     */
    private boolean joinStatus;

    /**
     * 创建者id
     */
    private Long userId;

    /**
     * 过期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date expireTime;

    /**
     * 最大人数
     */
    private Integer maxNums;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    /**
     * 状态 0表示公开 1表示私有 2表示加密
     */
    private Integer teamStatus;

    /**
     * 队伍用户信息
     */
    private List<UserVo> users;

    /**
     * 加入队伍的人数
     */
    private Integer joinNums;
}
