package com.zzy.team.constant;

import lombok.Getter;

@Getter
public enum ErrorStatus {

    PARAMS_ERROR(400, "参数错误", ""),
    UNAUTHORIZED_ERROR(401, "未授权", ""),
    FORBIDDEN_ERROR(403, "禁止访问", ""),
    SERVICE_ERROR(500, "服务器错误", "");

    private int code;
    private String message;
    private String description;

    ErrorStatus(int code, String message, String description) {
        this.code = code;
        this.message = message;
        this.description = description;
    }
}
