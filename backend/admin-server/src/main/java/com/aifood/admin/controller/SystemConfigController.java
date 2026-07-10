package com.aifood.admin.controller;

import com.ai.food.common.util.ApiResponse;
import com.aifood.admin.common.annotation.RequireAdmin;
import com.aifood.admin.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 系统配置管理接口：读取/更新全局限额配置。
 *
 * <p>由 {@link com.aifood.admin.common.AdminWebConfig} 拦截器统一鉴权，
 * 类级 {@link RequireAdmin} 作为冗余防御。</p>
 *
 * <ul>
 *   <li>GET  /admin/api/config/token-limit  — 查默认每日 token 限额</li>
 *   <li>PUT  /admin/api/config/token-limit  — 设置默认每日 token 限额</li>
 * </ul>
 */
@RestController
@RequestMapping("/admin/api/config")
@RequireAdmin
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    /**
     * 查询默认每日 token 限额（config_key = daily_token_limit_default）。
     *
     * @return 配置键值对，缺省回退 1000000
     */
    @GetMapping("/token-limit")
    public ApiResponse<Map<String, Object>> getTokenLimit() {
        String value = systemConfigService.getConfigValue("daily_token_limit_default");
        return ApiResponse.success(Map.of(
            "configKey", "daily_token_limit_default",
            "configValue", value != null ? value : "1000000"
        ));
    }

    /**
     * 更新默认每日 token 限额。
     *
     * @param body 请求体，key "value" 为新的限额值
     * @return 操作结果
     */
    @PutMapping("/token-limit")
    public ApiResponse<Map<String, Object>> updateTokenLimit(@RequestBody Map<String, Object> body) {
        String value = String.valueOf(body.get("value"));
        systemConfigService.setConfigValue("daily_token_limit_default", value, "默认每日 token 限额（per user）");
        return ApiResponse.success(Map.of("success", true, "configValue", value));
    }
}
