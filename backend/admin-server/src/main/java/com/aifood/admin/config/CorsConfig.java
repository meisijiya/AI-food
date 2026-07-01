package com.aifood.admin.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * 全局 CORS 配置。
 *
 * <p>Spring Security 的 SecurityFilterChain 优先于 Spring MVC CORS 处理,
 * 导致 OPTIONS preflight 在 Security 层就被 401 了。
 *
 * <p>解决:同时注册
 * <ol>
 *   <li>{@link CorsFilter} bean(类型 = CorsFilter,Spring Security 的 CorsConfigurer 找它)</li>
 *   <li>{@link FilterRegistrationBean} 把上面那个 filter 装到 servlet 链最前面</li>
 * </ol>
 *
 * <p>关键:bean name 必须是 {@code corsFilter},Spring Security 的 {@code CorsConfigurer}
 * 在调 {@code http.cors()} 时按 {@code "corsFilter"} 这个固定名找 bean。</p>
 */
@Configuration
public class CorsConfig {

    /**
     * 1. 注册 CorsFilter bean(名字固定为 "corsFilter",供 Spring Security 找)
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // ponytail: 用 addAllowedOriginPattern 而不是 addAllowedOrigin,
        // 前者支持通配符,后者在 allowCredentials=true 时不允许 *
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    /**
     * 2. 把上面的 CorsFilter 提到 servlet 链最前面(早于 Spring Security),
     * 这样 OPTIONS preflight 直接被 CORS filter 拦截处理,不进 Security。
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration(CorsFilter corsFilter) {
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(corsFilter);
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
