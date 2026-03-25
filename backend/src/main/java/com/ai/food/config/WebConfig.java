package com.ai.food.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:}")
    private String uploadDir;

    private final RateLimitInterceptor rateLimitInterceptor;

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
        if (uploadDir != null && !uploadDir.isBlank()) {
            Path p = Paths.get(uploadDir);
            if (p.isAbsolute()) return p.toString();
        }
        return Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath().toString();
    }
}
