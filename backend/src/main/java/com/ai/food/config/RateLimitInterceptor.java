package com.ai.food.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    private static final int LOGIN_LIMIT = 5;
    private static final int REGISTER_LIMIT = 3;
    private static final int CONVERSATION_LIMIT = 10;
    private static final int AI_CHAT_LIMIT = 10;
    private static final int AI_RECOMMEND_LIMIT = 30;
    private static final int AI_SIMILARITY_LIMIT = 60;
    private static final int AI_VALIDATE_LIMIT = 60;
    private static final long WINDOW_MS = 60_000;
    private static final String KEY_PREFIX = "rl:";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (!"POST".equals(method) && !"DELETE".equals(method)) {
            return true;
        }

        int limit = getLimit(path);
        if (limit <= 0) return true;

        String clientIp = getClientIp(request);
        String key = KEY_PREFIX + path + ":" + clientIp;

        try {
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(key, Duration.ofMillis(WINDOW_MS));
            }
            if (count != null && count > limit) {
                log.warn("Rate limit exceeded: {} {} from {}", method, path, clientIp);
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
                return false;
            }
        } catch (Exception e) {
            // ponytail: Redis 不可用时降级放行，避免阻塞业务
            log.warn("Rate limiter Redis unavailable, allowing request: {}", e.getMessage());
        }
        return true;
    }

    private int getLimit(String path) {
        if (path.contains("/auth/login")) return LOGIN_LIMIT;
        if (path.contains("/auth/register")) return REGISTER_LIMIT;
        if (path.contains("/conversation/start")) return CONVERSATION_LIMIT;
        if (path.contains("/api/ai/chat")) return AI_CHAT_LIMIT;
        if (path.contains("/api/ai/recommend")) return AI_RECOMMEND_LIMIT;
        if (path.contains("/api/ai/similarity")) return AI_SIMILARITY_LIMIT;
        if (path.contains("/api/ai/validate-answer")) return AI_VALIDATE_LIMIT;
        return 0;
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
