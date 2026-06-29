package com.ai.food.common.util;

import lombok.Getter;

/**
 * 统一 API 响应状态码枚举。
 * <p>
 * 用于 {@link ApiResponse} 的成功/失败工厂方法，保证 HTTP 业务码在前后端间一致。
 * 注意：本枚举的 code 字段是业务约定码，HTTP 状态码应通过 {@code @ResponseStatus}
 * 或 Servlet 过滤器另外映射，避免在 controller 中散落裸数字。
 */
@Getter
public enum ApiResponseCode {

    /** 操作成功 */
    SUCCESS(200, "success"),
    /** 客户端请求参数错误 */
    BAD_REQUEST(400, "bad request"),
    /** 未登录或 token 失效 */
    UNAUTHORIZED(401, "unauthorized"),
    /** 已登录但权限不足 */
    FORBIDDEN(403, "forbidden"),
    /** 资源不存在 */
    NOT_FOUND(404, "not found"),
    /** 服务端未捕获异常 */
    INTERNAL_ERROR(500, "internal server error");

    /** 业务响应码 */
    private final int code;
    /** 默认提示语，可被 {@link ApiResponse#success(String, Object)} / {@code fail(int, String)} 覆盖 */
    private final String message;

    ApiResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
