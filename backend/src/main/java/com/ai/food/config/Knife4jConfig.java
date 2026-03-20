package com.ai.food.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI美食推荐应用 API")
                        .description("AI美食推荐应用后端接口文档，提供智能对话、参数收集、美食推荐等功能")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("AI Food Team")
                                .email("support@aifood.com")
                                .url("https://aifood.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("开发环境"),
                        new Server()
                                .url("https://api.aifood.com")
                                .description("生产环境")
                ));
    }

    @Bean
    public GroupedOpenApi conversationApi() {
        return GroupedOpenApi.builder()
                .group("对话管理")
                .pathsToMatch("/api/conversation/**")
                .build();
    }

    @Bean
    public GroupedOpenApi recommendationApi() {
        return GroupedOpenApi.builder()
                .group("推荐服务")
                .pathsToMatch("/api/recommendation/**")
                .build();
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("所有接口")
                .pathsToMatch("/api/**")
                .build();
    }
}