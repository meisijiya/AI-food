package com.aifood.admin.common;

import lombok.Getter;

/**
 * 管理后台业务异常。
 * code 取 HTTP 语义码或自定义业务码,由 AdminExceptionHandler 统一转换为 ApiResponse。
 */
@Getter
public class AdminException extends RuntimeException {

    private final int code;

    public AdminException(int code, String message) {
        super(message);
        this.code = code;
    }
}
