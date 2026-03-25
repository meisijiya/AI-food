package com.ai.food.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Map<String, RateBucket> buckets = new ConcurrentHashMap<>();
    private static final long CLEANUP_INTERVAL_MS = 60_000;
    private long lastCleanup = System.currentTimeMillis();

    private static final int LOGIN_LIMIT = 5;
    private static final int REGISTER_LIMIT = 3;
    private static final int CONVERSATION_LIMIT = 10;
    private static final long WINDOW_MS = 60_000;

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
        String key = path + ":" + clientIp;

        RateBucket bucket = buckets.computeIfAbsent(key, k -> new RateBucket(WINDOW_MS, limit));

        if (!bucket.tryConsume()) {
            log.warn("Rate limit exceeded: {} {} from {}", method, path, clientIp);
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
            return false;
        }

        maybeCleanup();
        return true;
    }

    private int getLimit(String path) {
        if (path.contains("/auth/login")) return LOGIN_LIMIT;
        if (path.contains("/auth/register")) return REGISTER_LIMIT;
        if (path.contains("/conversation/start")) return CONVERSATION_LIMIT;
        return 0;
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void maybeCleanup() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup > CLEANUP_INTERVAL_MS) {
            lastCleanup = now;
            buckets.entrySet().removeIf(e -> e.getValue().isExpired(now));
        }
    }

    private static class RateBucket {
        private final long windowMs;
        private final int maxRequests;
        private long windowStart;
        private final AtomicInteger count;

        RateBucket(long windowMs, int maxRequests) {
            this.windowMs = windowMs;
            this.maxRequests = maxRequests;
            this.windowStart = System.currentTimeMillis();
            this.count = new AtomicInteger(0);
        }

        synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();
            if (now - windowStart >= windowMs) {
                windowStart = now;
                count.set(0);
            }
            return count.incrementAndGet() <= maxRequests;
        }

        boolean isExpired(long now) {
            return now - windowStart >= windowMs * 2;
        }
    }
}
