package com.genius.smartlight.common;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public CommonResult<String> handleServiceException(ServiceException ex) {
        return CommonResult.error(400, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public CommonResult<String> handleValidException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldError() != null
                ? ex.getBindingResult().getFieldError().getDefaultMessage()
                : "参数校验失败";
        return CommonResult.error(400, message);
    }

    @ExceptionHandler(Exception.class)
    public CommonResult<String> handleException(Exception ex) {
        return CommonResult.error(500, ex.getMessage());
    }
}