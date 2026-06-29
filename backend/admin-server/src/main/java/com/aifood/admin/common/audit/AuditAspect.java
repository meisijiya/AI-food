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

import java.time.LocalDateTime;
import java.util.Arrays;

/** 操作审计 AOP。拦截 @AuditLog 注解方法,记录 actor/action/target */
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

            Object[] args = pjp.getArgs();
            // 约定:第二个参数是 target id(@PathVariable Long id)
            if (args.length >= 2 && args[1] != null) {
                e.setTargetId(args[1].toString());
            }
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
