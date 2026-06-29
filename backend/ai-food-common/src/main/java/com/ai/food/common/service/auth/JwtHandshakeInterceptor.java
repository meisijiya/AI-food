package com.ai.food.common.service.auth;

import com.ai.food.common.service.auth.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;

    private static final String AUTH_COOKIE_NAME = "auth_token";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);

        if (token == null || !jwtService.isTokenValid(token)) {
            log.warn("WebSocket handshake rejected: invalid token");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        Long userId = jwtService.getUserId(token);
        attributes.put("userId", userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    private static final String SUB_PROTOCOL_PREFIX = "jwt.";

    /**
     * 从 Sec-WebSocket-Protocol 请求头解析 JWT。
     * 前端通过 `new WebSocket(url, ['jwt.' + token])` 传入，
     * token 不再出现在 URL query / nginx access log / 浏览器 history 中。
     *
     * 注意：{@code ServerHttpRequest.getHeaders()} 返回基类 {@link org.springframework.http.HttpHeaders}，
     * 不暴露 {@code getSecWebSocketProtocol()}，所以直接按 header 名取。
     */
    private String extractTokenFromSubProtocol(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst("Sec-WebSocket-Protocol");
        if (header == null || header.isBlank()) {
            return null;
        }
        // 多个子协议用逗号分隔；只取第一个以 jwt. 开头的
        for (String proto : header.split(",")) {
            String trimmed = proto.trim();
            if (trimmed.startsWith(SUB_PROTOCOL_PREFIX)) {
                String token = trimmed.substring(SUB_PROTOCOL_PREFIX.length());
                return token.isEmpty() ? null : token;
            }
        }
        return null;
    }

    private String extractToken(ServerHttpRequest request) {
        // 1) 子协议（优先 — 不写 URL，不进 access log）
        String token = extractTokenFromSubProtocol(request);
        if (token != null) {
            return token;
        }

        // 2) Cookie 兜底（保留入口供后续 HttpOnly cookie 迁移使用）
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            Cookie[] cookies = httpRequest.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (AUTH_COOKIE_NAME.equals(cookie.getName())) {
                        return cookie.getValue();
                    }
                }
            }
        }

        // 不再支持 URL query 中的 ?token= 参数（修复：避免 token 出现在 nginx access log / 浏览器 history）
        return null;
    }
}
