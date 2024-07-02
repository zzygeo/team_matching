package com.zzy.team.model.request;

import com.zzy.team.common.PageRequest;
import lombok.Data;

@Data
public class TeamPageRequest extends PageRequest {
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
     * 创建者id
     */
    private Long userId;

    /**
     * 最大人数
     */
    private Integer maxNums;

    /**
     * 状态 0表示公开 1表示私有 2表示加密
     */
    private Integer teamStatus;

    /**
     * 搜索字段，模糊搜索队伍名或者描述
     */
    private String searchText;
}

