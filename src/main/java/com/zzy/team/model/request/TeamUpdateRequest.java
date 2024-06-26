package com.zzy.team.model.request;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class TeamUpdateRequest {
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
     * 过期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date expireTime;

    /**
     * 状态 0表示公开 1表示私有 2表示加密
     */
    @TableField(value = "team_status")
    private Integer teamStatus;

    /**
     * 队伍密码
     */
    @TableField(value = "team_password")
    private String teamPassword;
}
