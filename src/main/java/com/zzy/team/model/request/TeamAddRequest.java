package com.zzy.team.model.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class TeamAddRequest {

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
     * 最大人数
     */
    private Integer maxNums;

    /**
     * 状态 0表示公开 1表示私有 2表示加密
     */
    private Integer teamStatus;

    /**
     * 队伍密码
     */
    private String teamPassword;
}
