package com.aifood.admin.common;

import com.ai.food.common.service.auth.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 关闭 common 模块 {@link JwtAuthenticationFilter} 在 admin-server 中的 servlet 自动注册。
 *
 * <p>admin-server 通过 {@code scanBasePackages} 扫描 {@code com.ai.food.common},会把
 * ai-food-app 用的 {@code JwtAuthenticationFilter}（{@code @Component} 的
 * {@code OncePerRequestFilter}）当作 Filter Bean,被 Spring Boot 自动注册到 servlet
 * 容器并对所有请求生效。该 filter 依赖 Redis token 白名单（app 登录时写入 {@code token:{userId}}）,
 * 而管理后台的 token 由 {@link com.aifood.admin.controller.AuthController} 颁发、从不写 Redis,
 * 故会被该 filter 误判为「已失效」拦在 {@code /admin/api/**} 之前。</p>
 *
 * <p>管理后台的鉴权完全由 {@link com.aifood.admin.common.interceptor.AdminInterceptor}
 * 完成（JWT 校验 + role=ADMIN,不依赖 Redis）,因此在本模块禁用该 filter 的注册即可。
 * 注意:仅取消 servlet 注册,Bean 仍然存在,不影响 common 在 ai-food-app 中的行为。</p>
 */
// ponytail: setEnabled(false) 只摘掉 servlet 注册,不删 Bean —— 删 Bean 会破坏 common 的
// 自动装配契约;这里用 FilterRegistrationBean 包裹既有 Bean 并置 enabled=false 是最小侵入做法。
@Configuration
public class AdminFilterConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> disableCommonJwtAuthFilter(
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(jwtAuthenticationFilter);
        registration.setEnabled(false);
        return registration;
    }
}
