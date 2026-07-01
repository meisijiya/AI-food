package com.aifood.admin.controller;

import com.ai.food.common.util.ApiResponse;
import com.aifood.admin.common.AdminException;
import com.aifood.admin.common.annotation.AuditLog;
import com.aifood.admin.common.annotation.RequireAdmin;
import com.aifood.admin.common.interceptor.AdminInterceptor;
import com.aifood.admin.dto.AdminUserVO;
import com.aifood.admin.dto.UpdateRoleReq;
import com.aifood.admin.dto.UserQueryReq;
import com.aifood.admin.service.UserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
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
    public ApiResponse<Page<AdminUserVO>> list(UserQueryReq req) {
        return ApiResponse.success(userService.pageVO(req));
    }

    /** 查询单个用户详情 */
    @GetMapping("/{id}")
    public ApiResponse<AdminUserVO> detail(@PathVariable Long id) {
        return ApiResponse.success(userService.getDetailVO(id));
    }

    /**
     * 修改用户角色。
     *
     * <p>防自降权:管理员不能把自己降为 USER,否则当前 token 立刻失效、
     * 系统中再也没有任何 ADMIN 可以恢复。返回 400。</p>
     */
    @PatchMapping("/{id}/role")
    @AuditLog(value = "修改用户角色", action = "UPDATE_USER_ROLE", targetParamIndex = 0)
    public ApiResponse<Void> updateRole(@PathVariable Long id,
                                       @Valid @RequestBody UpdateRoleReq req,
                                       HttpServletRequest httpReq) {
        // ponytail: 防自降权 —— 当前 admin 不能把自己改成 USER(只剩自己的 admin 锁死)
        Long currentAdminId = (Long) httpReq.getAttribute(AdminInterceptor.ATTR_ADMIN_ID);
        if (currentAdminId != null && currentAdminId.equals(id) && "USER".equals(req.getRole())) {
            throw new AdminException(400, "不能把自己降级为 USER,会失去唯一的 admin 账号");
        }
        userService.updateRole(id, req.getRole());
        return ApiResponse.success();
    }

    /** 禁用用户(软删,is_deleted=1) */
    @PostMapping("/{id}/disable")
    @AuditLog(value = "禁用用户", action = "DISABLE_USER", targetParamIndex = 0)
    public ApiResponse<Void> disable(@PathVariable Long id) {
        userService.disable(id);
        return ApiResponse.success();
    }

    /** 启用用户(is_deleted=0) */
    @PostMapping("/{id}/enable")
    @AuditLog(value = "启用用户", action = "ENABLE_USER", targetParamIndex = 0)
    public ApiResponse<Void> enable(@PathVariable Long id) {
        userService.enable(id);
        return ApiResponse.success();
    }
}
