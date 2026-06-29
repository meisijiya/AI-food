package com.ai.food.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 上传路径相关配置项，对应 application*.yml 中的 app.upload 段
 *
 * - baseUrl: 容器内 URL 路径前缀（如 /uploads）
 * - dir:     文件系统实际目录（开发期 ./uploads，生产期 /app/uploads）
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.upload")
public class UploadPathProperties {

    private String baseUrl;
    private String dir;
}
