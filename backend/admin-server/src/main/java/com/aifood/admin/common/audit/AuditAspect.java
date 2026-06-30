package com.aifood.admin.common.audit;

import com.aifood.admin.common.annotation.AuditLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 操作审计 AOP。拦截 @AuditLog 注解方法,记录 actor/action/target。
 *
 * <p>target_id 解析策略(优先级递减):</p>
 * <ol>
 *   <li>@AuditLog.targetParamIndex 显式声明的参数索引</li>
 *   <li>fallback: 找参数名为 "id" 的第一个参数</li>
 *   <li>fallback: 取 args[0](兼容旧调用)</li>
 * </ol>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogMapper auditLogMapper;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint pjp, AuditLog auditLog) throws Throwable {
        Long actorId = null;
        String username = "anonymous";
        try {
            HttpServletRequest req = currentRequest();
            if (req != null) {
                Object idAttr = req.getAttribute("adminId");
                if (idAttr instanceof Long) actorId = (Long) idAttr;
                Object userAttr = req.getAttribute("adminUsername");
                if (userAttr instanceof String) username = (String) userAttr;
            }
        } catch (Exception ignored) {}

        try {
            Object result = pjp.proceed();
            saveLog(actorId, username, auditLog, pjp, "SUCCESS", null);
            return result;
        } catch (Throwable t) {
            saveLog(actorId, username, auditLog, pjp, "FAIL", t.getMessage());
            throw t;
        }
    }

    private void saveLog(Long actorId, String username, AuditLog a,
                         ProceedingJoinPoint pjp, String status, String err) {
        try {
            AuditLogEntity e = new AuditLogEntity();
            e.setActorId(actorId != null ? actorId : 0L);
            e.setActorUsername(username != null ? username : "anonymous");
            e.setAction(a.action());
            e.setStatus(status);
            e.setErrorMessage(err);
            e.setCreatedAt(LocalDateTime.now());

            // ponytail: 用 annotation 的 targetParamIndex 显式取 target id,避免 args[1] 误取 req body
            Object[] args = pjp.getArgs();
            e.setTargetId(resolveTargetId(a, pjp, args));

            MethodSignature sig = (MethodSignature) pjp.getSignature();
            String className = sig.getMethod().getDeclaringClass().getSimpleName();
            if (className.endsWith("Controller")) {
                e.setTargetType(className.substring(0, className.length() - "Controller".length()).toUpperCase());
            } else {
                e.setTargetType(className.toUpperCase());
            }
            e.setPayload(String.format("method=%s, args=%s",
                pjp.getSignature().getName(), Arrays.toString(args)));

            HttpServletRequest req = currentRequest();
            if (req != null) {
                e.setIp(getClientIp(req));
                e.setUserAgent(req.getHeader("User-Agent"));
            }
            auditLogMapper.insert(e);
        } catch (Exception ex) {
            // ponytail: 审计失败不能阻塞业务
            log.error("保存审计日志失败", ex);
        }
    }

    /**
     * 解析 target id。
     * 优先级:@AuditLog.targetParamIndex → 参数名="id" → args[0]
     */
    private String resolveTargetId(AuditLog a, ProceedingJoinPoint pjp, Object[] args) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        Parameter[] params = method.getParameters();

        // 1. annotation 显式声明
        int idx = a.targetParamIndex();
        if (idx >= 0 && idx < args.length && args[idx] != null) {
            return args[idx].toString();
        }

        // 2. fallback: 找参数名为 "id" 的
        for (int i = 0; i < params.length && i < args.length; i++) {
            if ("id".equals(params[i].getName()) && args[i] != null) {
                return args[i].toString();
            }
        }

        // 3. fallback: args[0]
        if (args.length > 0 && args[0] != null) {
            return args[0].toString();
        }

        return null;
    }

    private HttpServletRequest currentRequest() {
        try {
            return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) return ip.split(",")[0].trim();
        ip = req.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty()) return ip;
        return req.getRemoteAddr();
    }
}
