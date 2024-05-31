package com.zzy.team.exception;

import com.zzy.team.constant.ErrorStatus;
import lombok.Data;

@Data
public class BusinessException extends RuntimeException {
    /**
     * 错误码
     */
    private Integer code;
    /**
     * 描述
     */
    private String description;

    public BusinessException(String message, Integer code, String description) {
        super(message);
        this.code = code;
        this.description = description;
    }

    public BusinessException(ErrorStatus errorStatus) {
        super(errorStatus.getMessage());
        this.code = errorStatus.getCode();
        this.description = errorStatus.getDescription();
    }

    public BusinessException(ErrorStatus errorStatus, String description) {
        super(errorStatus.getMessage());
        this.code = errorStatus.getCode();
        this.description = description;
    }
}

