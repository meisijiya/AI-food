package com.aifood.admin.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

/**
 * 管理后台 Spring Security 配置。
 *
 * <p>{@code /admin/api/**} 由 AdminInterceptor 完成 token + role 校验,
 * 故让 Spring Security 整条 filter chain 跳过这些路径(spring-boot-admin-starter-server
 * 默认会 require 匿名认证,对未带 token 的请求直接返回 401,把 AdminInterceptor
 * 拦在 servlet 链之前)。</p>
 *
 * <p>{@code /admin/sba/**} 是 spring-boot-admin-starter-server 自带的 UI 与监控接口,
 * 由它内置的 SecurityFilterChain 管理,本配置不动。</p>
 */
// ponytail: 用 web.ignoring() 让 Security 跳过 /admin/api/** 整个 filter 链(不是
// permitAll — 那还需要走 anonymous + authorization 过滤,会先于 AdminInterceptor 响应 401),
// 如需更细粒度(全局 SSO/CSRF)再扩展。
@Configuration
public class AdminSecurityConfig {

    @Bean
    public WebSecurityCustomizer adminApiIgnoringCustomizer() {
        return web -> web.ignoring()
                .requestMatchers("/admin/api/**", "/admin/api/auth/**");
    }
}
