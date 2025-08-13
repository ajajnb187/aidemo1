package org.example.ai.exception;


import lombok.extern.slf4j.Slf4j;
import org.example.ai.dto.ApiResponse;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<String> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", message);
        return ApiResponse.error(400, message);
    }

    /**
     * 参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ApiResponse<String> handleBindException(BindException e) {
        String message = e.getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数绑定失败: {}", message);
        return ApiResponse.error(400, message);
    }


    /**
     * 运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ApiResponse<String> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: ", e);
        return ApiResponse.error(e.getMessage());
    }

    /**
     * 通用异常
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<String> handleException(Exception e) {
        log.error("系统异常: ", e);
        return ApiResponse.error("系统异常，请联系管理员");
    }
}
