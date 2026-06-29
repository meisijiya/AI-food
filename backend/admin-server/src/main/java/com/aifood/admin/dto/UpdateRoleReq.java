package com.aifood.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 管理后台修改用户角色请求。仅允许 USER / ADMIN 两个枚举值 */
@Data
public class UpdateRoleReq {

    @NotBlank
    @Pattern(regexp = "USER|ADMIN", message = "role 必须是 USER 或 ADMIN")
    private String role;
}
