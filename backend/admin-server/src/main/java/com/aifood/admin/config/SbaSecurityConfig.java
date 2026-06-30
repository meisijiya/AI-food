package com.aifood.admin.config;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Spring Boot Admin Server 安全配置。
 *
 * <p>对 {@code /admin/sba/**} 启用 HTTP Basic + 表单登录,
 * 仅 ADMIN 角色可访问;其余路径放行(由 {@code common.AdminSecurityConfig}
 * 的 {@code WebSecurityCustomizer.ignoring()} 处理 {@code /admin/api/**},
 * 由 Druid 自身的 StatViewServlet 校验 {@code /admin/druid/**})。</p>
 *
 * <p>关闭 CSRF:SBA 自带 UI 通过 JSON 与服务端交互,登录态走 Basic Auth,
 * 额外 CSRF token 会破坏 SBA 的登录跳转。</p>
 */
// ponytail: 用 formLogin(httpBasic) 同时打开表单和 Basic,
// 这样浏览器访问会跳 SBA 自带登录页,API 客户端可以用 Basic。
// 角色和口令来自 application.yml 的 spring.security.user。
@Configuration
public class SbaSecurityConfig {

    /** Spring Boot Admin Server 配置属性(用于解析 context-path)。 */
    private final AdminServerProperties adminServer;

    /**
     * 构造函数注入 AdminServerProperties。
     *
     * @param adminServer SBA 配置属性
     */
    public SbaSecurityConfig(AdminServerProperties adminServer) {
        this.adminServer = adminServer;
    }

    /**
     * 配置 Spring Security 过滤链:SBA 路径需 ADMIN 角色,其余放行。
     *
     * @param http Spring Security 配置器
     * @return 配置完成的过滤链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // ponytail: SBA 自带登录页会读 redirectTo 参数决定登录后跳转目标
        SavedRequestAwareAuthenticationSuccessHandler success =
                new SavedRequestAwareAuthenticationSuccessHandler();
        success.setTargetUrlParameter("redirectTo");
        success.setDefaultTargetUrl(adminServer.path("/"));

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(adminServer.path("/assets/**")).permitAll()
                .requestMatchers(adminServer.path("/login")).permitAll()
                .requestMatchers(adminServer.path("/logout")).permitAll()
                .requestMatchers(adminServer.path("/actuator/health")).permitAll()
                .requestMatchers(adminServer.path("/actuator/info")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/admin/api/auth/login")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/admin/sba/**")).hasRole("ADMIN")
                .anyRequest().permitAll()
        ).formLogin(form -> form
                .loginPage(adminServer.path("/login"))
                .successHandler(success))
         .httpBasic(b -> {})
         .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
