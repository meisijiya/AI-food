package com.ai.food.config;

import com.ai.food.common.config.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final UploadPathProperties uploadPathProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String root = getUploadRoot();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + root + "/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**");
    }

    private String getUploadRoot() {
        // toAbsolutePath 统一处理相对路径（dev: ./uploads）和绝对路径（prod: /app/uploads）
        return Paths.get(uploadPathProperties.getDir()).toAbsolutePath().toString();
    }
}
