package com.aifood.admin.controller;

import com.ai.food.common.util.ApiResponse;
import com.aifood.admin.common.annotation.RequireAdmin;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * AI 模型列表接口。
 *
 * <p>单一端点:
 * <ul>
 *   <li>GET /admin/api/models — 返回当前生效的对话模型配置</li>
 * </ul>
 *
 * <p>模型配置通过 {@code application.yml} 的 {@code spring.ai.openai.*} 注入,
 * 缺省时回退到 deepseek-chat / deepseek baseUrl(仅作前端展示用,真实调用走 ai-food-app)。</p>
 *
 * <p>由 {@link com.aifood.admin.common.AdminWebConfig} 拦截器统一鉴权,
 * 类级 {@link RequireAdmin} 作为冗余防御。</p>
 */
@RestController
@RequestMapping("/admin/api/models")
@RequireAdmin
@RequiredArgsConstructor
public class ModelController {

    /** 当前对话模型名,从配置注入,缺省 deepseek-chat */
    @Value("${spring.ai.openai.chat.options.model:deepseek-chat}")
    private String defaultModel;

    /** 模型 base-url,从配置注入,缺省 DeepSeek 官方 */
    @Value("${spring.ai.openai.base-url:https://api.deepseek.com}")
    private String baseUrl;

    /** 返回当前生效的模型配置(单条) */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        // ponytail: 单模型配置直接硬编码为 1 条,不做多模型注册中心
        return ApiResponse.success(List.of(
                Map.of("name", defaultModel, "baseUrl", baseUrl, "active", true)
        ));
    }
}
