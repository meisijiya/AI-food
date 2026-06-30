package com.aifood.admin.controller;

import com.ai.food.common.util.ApiResponse;
import com.aifood.admin.common.annotation.RequireAdmin;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理后台监控接口(健康 / JVM 摘要)。
 *
 * <p>两个端点:
 * <ul>
 *   <li>GET /admin/api/monitor/health — Spring Boot Actuator 健康详情</li>
 *   <li>GET /admin/api/monitor/jvm    — JVM 内存 / CPU / 运行时信息</li>
 * </ul>
 *
 * <p>由 {@link com.aifood.admin.common.AdminWebConfig} 拦截器统一鉴权,
 * 类级 {@link RequireAdmin} 作为冗余防御。</p>
 */
@RestController
@RequestMapping("/admin/api/monitor")
@RequireAdmin
@RequiredArgsConstructor
public class MonitorController {

    /** Spring Boot Actuator 健康端点(由 actuator starter 提供) */
    private final HealthEndpoint healthEndpoint;

    /** 健康检查:直接透传 Actuator 的 HealthComponent */
    @GetMapping("/health")
    public ApiResponse<Object> health() {
        return ApiResponse.success(healthEndpoint.health());
    }

    /** JVM 摘要:堆内存 / CPU 核数 / 进程运行时长 */
    @GetMapping("/jvm")
    public ApiResponse<Map<String, Object>> jvm() {
        // ponytail: 仅暴露最小集,完整 JMX 留给 actuator/metrics;此处满足 admin 页面快速预览
        Runtime r = Runtime.getRuntime();
        Map<String, Object> info = new HashMap<>();
        info.put("totalMemory", r.totalMemory());
        info.put("freeMemory", r.freeMemory());
        info.put("maxMemory", r.maxMemory());
        info.put("availableProcessors", r.availableProcessors());
        info.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime());
        return ApiResponse.success(info);
    }
}
