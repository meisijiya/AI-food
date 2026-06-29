package com.aifood.admin.controller;

import com.ai.food.common.model.SysUser;
import com.ai.food.common.util.ApiResponse;
import com.aifood.admin.common.annotation.AuditLog;
import com.aifood.admin.common.annotation.RequireAdmin;
import com.aifood.admin.dto.UpdateRoleReq;
import com.aifood.admin.dto.UserQueryReq;
import com.aifood.admin.service.UserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台用户管理接口。
 *
 * <p>所有接口由 {@link com.aifood.admin.common.AdminWebConfig} 拦截器统一鉴权
 * （除 /login 外 /admin/api/** 全部要求 ADMIN token 有效）。类级别 {@link RequireAdmin}
 * 是冗余防御层——拦截器已生效,这里同时保留以与 AuthController 风格一致。</p>
 *
 * <p>5 个接口:
 * <ul>
 *   <li>GET    /admin/api/users              分页列表</li>
 *   <li>GET    /admin/api/users/{id}         详情</li>
 *   <li>PATCH  /admin/api/users/{id}/role    修改角色 (审计:UPDATE_USER_ROLE)</li>
 *   <li>POST   /admin/api/users/{id}/disable 禁用   (审计:DISABLE_USER)</li>
 *   <li>POST   /admin/api/users/{id}/enable  启用   (审计:ENABLE_USER)</li>
 * </ul>
 */
@RestController
@RequestMapping("/admin/api/users")
@RequireAdmin
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 分页查询用户列表 */
    @GetMapping
    public ApiResponse<Page<SysUser>> list(UserQueryReq req) {
        return ApiResponse.success(userService.page(req));
    }

    /** 查询单个用户详情 */
    @GetMapping("/{id}")
    public ApiResponse<SysUser> detail(@PathVariable Long id) {
        return ApiResponse.success(userService.getDetail(id));
    }

    /** 修改用户角色 */
    @PatchMapping("/{id}/role")
    @AuditLog(value = "修改用户角色", action = "UPDATE_USER_ROLE")
    public ApiResponse<Void> updateRole(@PathVariable Long id, @Valid @RequestBody UpdateRoleReq req) {
        userService.updateRole(id, req.getRole());
        return ApiResponse.success();
    }

    /** 禁用用户(软删,is_deleted=1) */
    @PostMapping("/{id}/disable")
    @AuditLog(value = "禁用用户", action = "DISABLE_USER")
    public ApiResponse<Void> disable(@PathVariable Long id) {
        userService.disable(id);
        return ApiResponse.success();
    }

    /** 启用用户(is_deleted=0) */
    @PostMapping("/{id}/enable")
    @AuditLog(value = "启用用户", action = "ENABLE_USER")
    public ApiResponse<Void> enable(@PathVariable Long id) {
        userService.enable(id);
        return ApiResponse.success();
    }
}
