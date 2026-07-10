package com.aifood.admin.controller;

import com.ai.food.common.util.ApiResponse;
import com.aifood.admin.common.annotation.RequireAdmin;
import com.aifood.admin.service.UserTokenQuotaService;
import com.ai.food.common.model.UserTokenQuota;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * per-user token 额度管理接口。
 *
 * <p>由 {@link com.aifood.admin.common.AdminWebConfig} 拦截器统一鉴权，
 * 类级 {@link RequireAdmin} 作为冗余防御。</p>
 *
 * <ul>
 *   <li>GET    /admin/api/token-quota/{userId} — 查询用户限额覆盖</li>
 *   <li>PUT    /admin/api/token-quota/{userId} — 设置用户限额覆盖</li>
 *   <li>DELETE /admin/api/token-quota/{userId} — 删除用户限额覆盖</li>
 * </ul>
 */
@RestController
@RequestMapping("/admin/api/token-quota")
@RequireAdmin
@RequiredArgsConstructor
public class TokenQuotaController {

    private final UserTokenQuotaService userTokenQuotaService;

    /**
     * 查询用户的限额覆盖记录。
     *
     * @param userId 用户 ID
     * @return 限额信息，{@code isOverride=false} 表示未覆盖
     */
    @GetMapping("/{userId}")
    public ApiResponse<Map<String, Object>> getByUserId(@PathVariable Long userId) {
        UserTokenQuota quota = userTokenQuotaService.getByUserId(userId);
        if (quota == null) {
            return ApiResponse.success(Map.of("userId", userId, "dailyTokenLimit", null, "isOverride", false));
        }
        return ApiResponse.success(Map.of(
            "userId", quota.getUserId(),
            "dailyTokenLimit", quota.getDailyTokenLimit(),
            "isOverride", true
        ));
    }

    /**
     * 设置/更新用户限额覆盖。
     *
     * @param userId 用户 ID
     * @param body   请求体，key "dailyTokenLimit" 为限额值
     * @return 操作结果
     */
    @PutMapping("/{userId}")
    public ApiResponse<Map<String, Object>> upsert(@PathVariable Long userId, @RequestBody Map<String, Object> body) {
        int dailyLimit = ((Number) body.get("dailyTokenLimit")).intValue();
        userTokenQuotaService.upsert(userId, dailyLimit);
        return ApiResponse.success(Map.of("success", true, "userId", userId, "dailyTokenLimit", dailyLimit));
    }

    /**
     * 删除用户限额覆盖，恢复全局默认。
     *
     * @param userId 用户 ID
     * @return 操作结果
     */
    @DeleteMapping("/{userId}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable Long userId) {
        userTokenQuotaService.delete(userId);
        return ApiResponse.success(Map.of("success", true, "userId", userId, "deleted", true));
    }
}
