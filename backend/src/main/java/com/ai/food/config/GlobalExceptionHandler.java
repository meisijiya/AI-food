package com.ai.food.config;

import com.ai.food.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e) {
        String msg = e.getMessage();
        log.warn("Business error: {}", msg);

        if (msg == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("服务器内部错误"));
        }

        if (msg.contains("不存在") || msg.contains("not found") || msg.contains("已被删除")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("请求的资源不存在"));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("操作失败，请稍后重试"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("服务器内部错误"));
    }
}
