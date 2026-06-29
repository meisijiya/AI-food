package com.aifood.admin.common;

import com.ai.food.common.util.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 管理后台全局异常处理。
 * 仅处理 com.aifood.admin 包下的 Controller 抛出的异常。
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.aifood.admin")
public class AdminExceptionHandler {

    /** 业务异常 — 透传 code 和 message */
    @ExceptionHandler(AdminException.class)
    public ApiResponse<Void> handleAdmin(AdminException e) {
        log.warn("AdminException: {} {}", e.getCode(), e.getMessage());
        return ApiResponse.fail(e.getCode(), e.getMessage());
    }

    /** 兜底 — 任何未捕获异常都返回 500,不暴露堆栈给调用方 */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleAny(Exception e) {
        log.error("Unexpected error", e);
        return ApiResponse.fail(500, "服务器内部错误: " + e.getMessage());
    }
}
