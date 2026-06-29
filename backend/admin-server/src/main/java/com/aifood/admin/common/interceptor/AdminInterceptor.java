package com.aifood.admin.common.interceptor;

import com.ai.food.common.mapper.UserMapper;
import com.ai.food.common.model.SysUser;
import com.ai.food.common.service.auth.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理后台权限拦截器:校验 JWT + 校验 sys_user.role = ADMIN。
 * 通过后把 adminId / adminUsername 写入 request 属性,供后续 Controller 读取。
 *
 * <p>拦截器抛出的异常不会自动走 {@code @RestControllerAdvice},所以在这里直接
 * 写 JSON 响应并返回 {@code false},避免依赖额外的异常处理链。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {

    /** 写入 request 的当前管理员 ID,Controller 可按需取用 */
    public static final String ATTR_ADMIN_ID = "adminId";
    /** 写入 request 的当前管理员用户名,Controller 可按需取用 */
    public static final String ATTR_ADMIN_USERNAME = "adminUsername";

    private final JwtService jwtService;
    /** 实为 sys_user 表的 mapper（common 中命名沿用 UserMapper） */
    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
        // 预检请求直接放行,避免 CORS 握手被拦截
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            return true;
        }
        String token = extractToken(req);
        if (token == null) {
            writeError(resp, 401, "未登录");
            return false;
        }
        if (!jwtService.isTokenValid(token)) {
            writeError(resp, 401, "Token 已过期");
            return false;
        }
        Long userId = jwtService.getUserId(token);
        SysUser user = userMapper.selectById(userId);
        // 用户不存在或角色非 ADMIN,一律视为越权
        if (user == null || !"ADMIN".equals(user.getRole())) {
            writeError(resp, 403, "需要管理员权限");
            return false;
        }
        req.setAttribute(ATTR_ADMIN_ID, userId);
        req.setAttribute(ATTR_ADMIN_USERNAME, user.getUsername());
        return true;
    }

    /**
     * 直接把 ApiResponse 形状的 JSON 写到 response,覆盖默认的 500 错误页。
     * ponytail: 手拼 JSON 而非 Jackson — 拦截器失败路径追求可读+轻量,引 ObjectMapper
     * 多一个 NPE 风险(Map.of 不允许 null values)。
     */
    private void writeError(HttpServletResponse resp, int code, String message) {
        try {
            resp.setStatus(code);
            resp.setContentType("application/json;charset=UTF-8");
            // 与 ApiResponse 保持一致:{code, message, data}
            resp.getWriter().write(
                    "{\"code\":" + code
                            + ",\"message\":\"" + escapeJson(message) + "\""
                            + ",\"data\":null}");
        } catch (Exception ex) {
            log.warn("AdminInterceptor writeError failed", ex);
        }
    }

    /** 简单 JSON 字符串转义,只处理双引号与反斜杠(中文 UTF-8 字节不需转义) */
    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') sb.append('\\').append(c);
            else sb.append(c);
        }
        return sb.toString();
    }

    /**
     * 提取 token:优先取 Authorization: Bearer xxx,其次取名为 admin_token 的 cookie。
     */
    private String extractToken(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("admin_token".equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }
}
