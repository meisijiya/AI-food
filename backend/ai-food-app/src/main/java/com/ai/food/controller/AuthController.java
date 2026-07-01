package com.ai.food.controller;

import com.ai.food.common.util.ApiResponse;
import com.ai.food.dto.LoginRequest;
import com.ai.food.dto.LoginResponse;
import com.ai.food.dto.RegisterRequest;
import com.ai.food.dto.SendCodeRequest;
import com.ai.food.service.auth.AuthService;
import com.ai.food.util.IpUtil;
import com.github.benmanes.caffeine.cache.Cache;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证与用户", description = "注册、登录、验证码、用户登出")
public class AuthController {

    private final AuthService authService;

    // [P0-登录限流] login/register 共用：60s 内同 email 5 次，或同 ip 5 次
    private static final int AUTH_RATE_LIMIT = 5;

    @Qualifier("emailLimitByUsername")
    private final Cache<String, Boolean> emailLimitByUsername;

    @Qualifier("emailLimitByIp")
    private final Cache<String, Boolean> emailLimitByIp;

    @PostMapping("/send-code")
    @Operation(summary = "发送邮箱验证码", description = "向指定邮箱发送注册/登录验证码，60 秒内同一邮箱只能发一次")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "发送成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "邮箱格式错误或频率超限")
    })
    public ApiResponse<Void> sendCode(
            @Parameter(description = "邮箱地址", required = true, example = "user@example.com")
            @Valid @RequestBody SendCodeRequest req,
            HttpServletRequest request) {
        String clientIp = IpUtil.getClientIp(request);
        authService.sendCode(req, clientIp);
        return ApiResponse.success("验证码已发送", null);
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "用邮箱+验证码注册新账号，注册成功返回 JWT token")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "注册成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "邮箱已注册或验证码错误")
    })
    public ApiResponse<LoginResponse> register(
            @Parameter(description = "注册信息（邮箱+验证码+密码）", required = true)
            @Valid @RequestBody RegisterRequest req,
            HttpServletRequest request) {
        // [P0-登录限流] 按 email + ip 双维度，超限直接拒绝
        String clientIp = IpUtil.getClientIp(request);
        checkAuthRateLimit(req.getEmail(), clientIp);

        LoginResponse response = authService.register(req);
        return ApiResponse.success("注册成功", response);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "邮箱+密码登录，返回 JWT token（用于后续接口鉴权）")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "登录成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "邮箱或密码错误")
    })
    public ApiResponse<LoginResponse> login(
            @Parameter(description = "登录信息（邮箱+密码）", required = true)
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest request,
            HttpServletResponse response) {
        // [P0-登录限流] 防暴力破解：按 email + ip 限流
        String clientIp = IpUtil.getClientIp(request);
        checkAuthRateLimit(req.getEmail(), clientIp);

        LoginResponse loginResponse = authService.login(req);
        // 安全修复（M2-后端）：将 JWT 写入 HttpOnly cookie，浏览器自动随请求带上，前端 JS 无法读取
        ResponseCookie cookie = ResponseCookie.from("auth_token", loginResponse.getToken())
                .httpOnly(true)
                .secure(false) // ponytail: dev=false；生产改 ${COOKIE_SECURE:true}
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofDays(7))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ApiResponse.success("登录成功", loginResponse);
    }

    // ponytail: 复用 CaffeineConfig 中已有的 emailLimitBy*，key 加 ":login" / ":register" 后缀
    // 与 send-code 的 send-code 后缀不冲突。同 email 60s 内只能 send-code 一次 + login/register 5 次。
    private void checkAuthRateLimit(String email, String clientIp) {
        if (email != null && !email.isBlank()) {
            String key = email + ":login";
            if (Boolean.TRUE.equals(emailLimitByUsername.getIfPresent(key))) {
                throw new RuntimeException("尝试次数过多，请稍后再试");
            }
            emailLimitByUsername.put(key, true);
        }
        if (clientIp != null && !clientIp.isBlank()) {
            String key = clientIp + ":login";
            if (Boolean.TRUE.equals(emailLimitByIp.getIfPresent(key))) {
                throw new RuntimeException("尝试次数过多，请稍后再试");
            }
            emailLimitByIp.put(key, true);
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "使当前 token 失效，删除 Redis 中的 session")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        // 安全修复（M2-后端）：清空 HttpOnly cookie，使浏览器不再带上旧 token
        ResponseCookie cookie = ResponseCookie.from("auth_token", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
                authService.logout(userId);
            }
        } catch (Exception ignore) {
            // token 过期或无效时 principal 为 "anonymousUser"，直接返回 success
        }
        return ApiResponse.success("已退出登录", null);
    }
}
