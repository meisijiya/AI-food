package com.aifood.admin.config;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Actuator 端点安全配置。
 *
 * <p>Spring Boot Admin 自带的 SbaSecurityConfig 会匹配 /admin/sba/** 但放过其他路径。
 * 这里的 filterChain 显式锁住 /actuator/** (除 health),防止:
 * <ul>
 *   <li>未授权访问 /actuator/env(泄露 JWT_SECRET/DB_PASSWORD/DEEPSEEK_API_KEY 等所有环境变量)</li>
 *   <li>未授权访问 /actuator/metrics(泄露内部指标)</li>
 *   <li>/actuator/health 看到数据库/Redis 类型/版本细节</li>
 * </ul>
 *
 * <p>Health 端点保持公开,因为 Docker / k8s 需要它做探活。
 *
 * <p>用 {@code @Order(0)} 优先于 SBA 的 SecurityFilterChain 匹配,避免被它先拦截。
 */
@Configuration
public class ActuatorSecurityConfig {

    /**
     * Actuator 路径安全过滤链。
     * - /actuator/health 公开(ping 检查需要)
     * - /actuator/info 公开(只暴露 app.name/description,无敏感信息)
     * - 其他 /actuator/** 需 basic auth
     */
    @Bean
    @Order(0)
    public SecurityFilterChain actuatorFilterChain(HttpSecurity http) throws Exception {
        // ponytail: 同 SbaSecurityConfig,不调 http.cors() 避免 bean 解析冲突
        // CorsFilter 已在 CorsConfig 中以 HIGHEST_PRECEDENCE 注册,提前处理 OPTIONS

        http.securityMatcher("/actuator/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .anyRequest().hasRole("ADMIN")
            )
            .httpBasic(b -> {})
            .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
