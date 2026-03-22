package com.ai.food.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String root = getUploadRoot();
        // /uploads/** 映射到文件系统 uploads 目录
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + root + "/");
    }

    private String getUploadRoot() {
        if (uploadDir != null && !uploadDir.isBlank()) {
            Path p = Paths.get(uploadDir);
            if (p.isAbsolute()) return p.toString();
        }
        return Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath().toString();
    }
}
