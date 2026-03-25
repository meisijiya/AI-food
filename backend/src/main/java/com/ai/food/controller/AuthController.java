package com.ai.food.controller;

import com.ai.food.dto.ApiResponse;
import com.ai.food.dto.LoginRequest;
import com.ai.food.dto.LoginResponse;
import com.ai.food.dto.RegisterRequest;
import com.ai.food.dto.SendCodeRequest;
import com.ai.food.service.auth.AuthService;
import com.ai.food.util.IpUtil;
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
public class AuthController {

    private final AuthService authService;

    @PostMapping("/send-code")
    public ApiResponse<Void> sendCode(@Valid @RequestBody SendCodeRequest req, HttpServletRequest request) {
        String clientIp = IpUtil.getClientIp(request);
        authService.sendCode(req, clientIp);
        return ApiResponse.success("验证码已发送", null);
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest req) {
        LoginResponse response = authService.register(req);
        return ApiResponse.success("注册成功", response);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        LoginResponse response = authService.login(req);
        return ApiResponse.success("登录成功", response);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            Long userId = Long.parseLong(authentication.getPrincipal().toString());
            authService.logout(userId);
        }
        return ApiResponse.success("已退出登录", null);
    }
}
