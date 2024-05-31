package com.zzy.team.exception;

import com.zzy.team.common.Result;
import com.zzy.team.constant.ErrorStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public Result businessExceptionHandler(BusinessException e){
        log.error("businessException:", e.getMessage(), e);
        return Result.FAIL(e.getCode(), e.getMessage(), e.getDescription());
    }

    @ExceptionHandler(RuntimeException.class)
    public Result runtimeExceptionHandler(RuntimeException e) {
        log.error("runtimeException:", e);
        return Result.FAIL(ErrorStatus.SERVICE_ERROR, e.getMessage(), "");
    }
}
