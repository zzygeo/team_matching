package com.zzy.team.common;

import com.zzy.team.constant.ErrorStatus;
import lombok.Data;

import java.io.Serializable;

@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = -7955290664472007185L;

    private int code;

    private T data;

    private String message;

    private String description;

    public Result(int code, T data, String message, String description) {
        this.code = code;
        this.data = data;
        this.message = message;
        this.description = description;
    }

    public static <T> Result<T> OK() {
        return new Result<>(200, null, "ok", "请求成功");
    }

    public static <T> Result<T> OK(T data) {
        return new Result<>(200, data, "ok", "请求成功");
    }

    public static <T> Result<T> OK(String description, T data) { return new Result<>(200, data, "ok", description);}

    public static <T> Result<T> FAIL(ErrorStatus errorStatus) {
        return new Result<>(errorStatus.getCode(), null, errorStatus.getMessage(), errorStatus.getDescription());
    }

    public static <T> Result<T> FAIL(ErrorStatus errorStatus, String description) {
        return new Result<>(errorStatus.getCode(), null, errorStatus.getMessage(), description);
    }

    public static <T> Result<T> FAIL(ErrorStatus errorStatus, String message, String description) {
        return new Result<>(errorStatus.getCode(), null, message, description);
    }

    public static <T> Result<T> FAIL(int code, String message, String description) {
        return new Result<>(code, null, message, description);
    }
}
