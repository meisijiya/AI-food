package com.aifood.admin.controller;

import com.ai.food.common.mapper.UserMapper;
import com.ai.food.common.model.SysUser;
import com.ai.food.common.service.auth.JwtService;
import com.ai.food.common.util.ApiResponse;
import com.aifood.admin.common.AdminException;
import com.aifood.admin.common.interceptor.AdminInterceptor;
import com.aifood.admin.dto.AdminUserVO;
import com.aifood.admin.dto.LoginReq;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台认证:登录 / 当前用户 / 登出。
 *
 * <p>/login 由 {@link com.aifood.admin.common.AdminWebConfig} 排除拦截、由
 * {@link com.aifood.admin.common.AdminSecurityConfig} 经 web.ignoring 放行,可匿名访问;
 * /me 与 /logout 仍走 {@link AdminInterceptor} 完成 token + role 校验。</p>
 */
// ponytail: common 中实为 sys_user 的 mapper 沿用 UserMapper 命名,提供 findByUsername/findByEmail。
@Slf4j
@RestController
@RequestMapping("/admin/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserMapper userMapper;
    private final JwtService jwtService;
    // 与 ai-food-app 一致:BCrypt 默认 rounds=10,可校验同一编码的密码
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/login")
    public ApiResponse<LoginResp> login(@Valid @RequestBody LoginReq req) {
        // ponytail: 支持用户名或邮箱登录(部分管理员实际用 email 登录)
        SysUser user = userMapper.findByUsername(req.getUsername());
        if (user == null) {
            user = userMapper.findByEmail(req.getUsername());
        }
        if (user == null) {
            throw new AdminException(401, "用户名或密码错误");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new AdminException(401, "用户名或密码错误");
        }
        if (!"ADMIN".equals(user.getRole())) {
            throw new AdminException(403, "该用户不是管理员");
        }
        String token = jwtService.generateToken(user.getId(), user.getUsername());
        LoginResp resp = new LoginResp();
        resp.setToken(token);
        resp.setAdminUser(AdminUserVO.from(user));
        log.info("admin login ok: id={} username={}", user.getId(), user.getUsername());
        return ApiResponse.success(resp);
    }

    @GetMapping("/me")
    public ApiResponse<AdminUserVO> me(HttpServletRequest req) {
        Long adminId = (Long) req.getAttribute(AdminInterceptor.ATTR_ADMIN_ID);
        SysUser user = userMapper.selectById(adminId);
        return ApiResponse.success(AdminUserVO.from(user));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // 无状态 JWT:登出仅由前端丢弃 token,服务端无需额外处理
        return ApiResponse.success();
    }

    @Data
    public static class LoginResp {
        private String token;
        private AdminUserVO adminUser;
    }
}
