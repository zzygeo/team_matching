package com.zzy.team.common;

import lombok.Data;

@Data
public class PageRequest {
    /**
     * 当前页码
     */
    protected Integer pageNum = 1;

    /**
     * 每页数量
     */
    protected Integer pageSize = 10;
}
