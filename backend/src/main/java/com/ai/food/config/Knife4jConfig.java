package com.ai.food.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Knife4j (Swagger) 全局配置
 *
 * 访问地址：
 *   - Knife4j UI:    http://localhost:8080/doc.html
 *   - OpenAPI JSON:  http://localhost:8080/v3/api-docs
 *   - OpenAPI 分组:  http://localhost:8080/v3/api-docs/{groupName}
 *
 * 分组说明（每个 GroupedOpenApi 对应一个菜单分类）：
 *   - 0-所有接口
 *   - 1-认证与用户
 *   - 2-AI 服务
 *   - 3-对话管理
 *   - 4-Feed 推荐
 *   - 5-社交（点赞/关注/通知）
 *   - 6-聊天与文件
 *   - 7-Bloom 去重
 */
@Configuration
public class Knife4jConfig {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI 美食推荐应用 API")
                        .description("AI 美食推荐与社交平台后端接口文档。\n\n" +
                                "## 核心特性\n" +
                                "- 🤖 AI 多轮对话推荐（7 个参数收集 → LLM 生成）\n" +
                                "- 📱 完整社交：WebSocket 聊天 + Redis Lua 点赞 + 布隆过滤器去重\n" +
                                "- 🔍 Redis 7 + Caffeine 多级缓存\n" +
                                "- 🔐 JWT 鉴权 + Redisson 分布式锁 + IP 限流\n" +
                                "- 🛠 Spring AI 1.0.0-M6 集成 DeepSeek\n\n" +
                                "## 在线试用\n" +
                                "1. 用 [/api/auth/send-code](#/认证与用户/sendCode) 获取验证码\n" +
                                "2. 用 [/api/auth/register](#/认证与用户/register) 注册\n" +
                                "3. 用 [/api/auth/login](#/认证与用户/login) 登录获取 token\n" +
                                "4. 点击右上角「🔓 Authorize」输入 Bearer + token\n" +
                                "5. 调用需要鉴权的接口")
                        .version("2.2.0")
                        .contact(new Contact()
                                .name("AI Food Team")
                                .email("support@aifood.com")
                                .url("https://github.com/meisijiya/AI-food"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .externalDocs(new ExternalDocumentation()
                        .description("AI-food GitHub 仓库")
                        .url("https://github.com/meisijiya/AI-food"))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("本地开发环境"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("Docker Compose（容器间访问 backend:8080）")
                ))
                .tags(List.of(
                        new Tag().name("认证与用户").description("注册、登录、验证码、用户信息"),
                        new Tag().name("AI 服务").description("AI 聊天、参数验证、推荐生成、相似度计算"),
                        new Tag().name("对话管理").description("会话状态、参数收集"),
                        new Tag().name("Feed 推荐").description("推荐发布、Feed 流、热榜、推荐生成"),
                        new Tag().name("点赞与社交").description("点赞、关注、通知、分享"),
                        new Tag().name("聊天与文件").description("WebSocket 聊天、图片/文件上传、记录打卡"),
                        new Tag().name("Bloom 过滤器").description("用户相似度、布隆过滤器持久化")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)
                                        .name("Authorization")
                                        .description("JWT 鉴权。格式：Bearer {token}"))
                )
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }

    /** 0-所有接口（一级菜单） */
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("0-所有接口")
                .pathsToMatch("/api/**")
                .build();
    }

    /** 1-认证与用户 */
    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("1-认证与用户")
                .pathsToMatch("/api/auth/**", "/api/user/**", "/api/guest/**")
                .build();
    }

    /** 2-AI 服务 */
    @Bean
    public GroupedOpenApi aiApi() {
        return GroupedOpenApi.builder()
                .group("2-AI服务")
                .pathsToMatch("/api/ai/**", "/api/recommendation/**")
                .build();
    }

    /** 3-对话管理 */
    @Bean
    public GroupedOpenApi conversationApi() {
        return GroupedOpenApi.builder()
                .group("3-对话管理")
                .pathsToMatch("/api/conversation/**")
                .build();
    }

    /** 4-Feed 推荐 */
    @Bean
    public GroupedOpenApi feedApi() {
        return GroupedOpenApi.builder()
                .group("4-Feed推荐")
                .pathsToMatch("/api/feed/**", "/api/record/**")
                .build();
    }

    /** 5-社交（点赞/关注/通知/分享） */
    @Bean
    public GroupedOpenApi socialApi() {
        return GroupedOpenApi.builder()
                .group("5-社交")
                .pathsToMatch("/api/like/**", "/api/follow/**", "/api/notification/**", "/api/share/**")
                .build();
    }

    /** 6-聊天与文件 */
    @Bean
    public GroupedOpenApi chatApi() {
        return GroupedOpenApi.builder()
                .group("6-聊天与文件")
                .pathsToMatch("/api/chat/**", "/api/upload/**")
                .build();
    }

    /** 7-Bloom 过滤器 */
    @Bean
    public GroupedOpenApi bloomApi() {
        return GroupedOpenApi.builder()
                .group("7-Bloom过滤器")
                .pathsToMatch("/api/bloom/**")
                .build();
    }
}
