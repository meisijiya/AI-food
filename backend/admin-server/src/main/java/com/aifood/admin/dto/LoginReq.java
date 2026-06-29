package com.aifood.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 管理后台登录请求。username 字段同时支持用户名或邮箱。 */
@Data
public class LoginReq {

    /** 用户名 或 邮箱 */
    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
