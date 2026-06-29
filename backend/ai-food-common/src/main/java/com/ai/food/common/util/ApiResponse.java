package com.ai.food.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 统一 API 响应包装。
 * <p>
 * 所有 controller 出口必须用 {@code ApiResponse} 包装，禁止直接返回裸对象。
 * - {@code data == null} 时通过 {@link JsonInclude} 在序列化时省略字段，
 *   减少响应体冗余（与 axios interceptor 解析约定一致）。
 * - 构造器私有化：调用方必须走 {@code success(...)} / {@code fail(...)} 工厂方法，
 *   避免散落裸数字 code。
 */
@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /** 业务响应码（与 {@link ApiResponseCode} 对应） */
    private int code;
    /** 提示语，失败时给前端展示 */
    private String message;
    /** 业务数据，{@code null} 时序列化时省略 */
    private T data;

    /** 无参成功（如：删除/确认类操作） */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(ApiResponseCode.SUCCESS.getCode(), ApiResponseCode.SUCCESS.getMessage(), null);
    }

    /** 仅返回数据，默认 message */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ApiResponseCode.SUCCESS.getCode(), ApiResponseCode.SUCCESS.getMessage(), data);
    }

    /** 自定义 message + 数据（如：登录返回 token） */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(ApiResponseCode.SUCCESS.getCode(), message, data);
    }

    /** 业务失败：自定义 code + message（用于业务侧特殊状态码，不在 {@link ApiResponseCode} 枚举内） */
    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    /** 业务失败：使用枚举预定义 code（如 401/403/500） */
    public static <T> ApiResponse<T> fail(ApiResponseCode code) {
        return new ApiResponse<>(code.getCode(), code.getMessage(), null);
    }
}
