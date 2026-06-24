package com.ai.food.controller;

import com.ai.food.dto.ApiResponse;
import com.ai.food.dto.LoginRequest;
import com.ai.food.dto.LoginResponse;
import com.ai.food.dto.RegisterRequest;
import com.ai.food.dto.SendCodeRequest;
import com.ai.food.service.auth.AuthService;
import com.ai.food.util.IpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证与用户", description = "注册、登录、验证码、用户登出")
public class AuthController {

    private final AuthService authService;

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
            @Valid @RequestBody RegisterRequest req) {
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
            @Valid @RequestBody LoginRequest req) {
        LoginResponse response = authService.login(req);
        return ApiResponse.success("登录成功", response);
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "使当前 token 失效，删除 Redis 中的 session")
    public ApiResponse<Void> logout() {
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
