package com.aifood.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI-Food 管理后台启动类。
 *
 * <p>负责启动 Spring Boot Admin Server（监控 UI）、Druid 监控 Servlet，
 * 以及后续将加入的 admin 后台业务控制器。本模块与 ai-food-app 共享
 * {@code com.ai.food.common} 下的 entity / mapper / jwt / ratelimit 等基础设施。</p>
 *
 * <p>通过 {@code scanBasePackages} 同时扫描本模块 ({@code com.aifood.admin})
 * 与 common ({@code com.ai.food.common})，确保 common 中的
 * {@code @Component} / {@code @Configuration} 都能被注册到 admin 上下文。</p>
 */
@EnableAspectJAutoProxy
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.aifood.admin", "com.ai.food.common"})
@MapperScan({"com.ai.food.common.mapper", "com.aifood.admin.common.audit"})
public class AdminApplication {

    /**
     * Spring Boot 应用入口。
     *
     * @param args 命令行参数，会原样透传给 SpringApplication
     */
    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
