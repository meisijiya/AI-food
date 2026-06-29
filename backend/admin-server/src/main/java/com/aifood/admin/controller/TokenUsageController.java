package com.aifood.admin.controller;

import com.aifood.admin.common.annotation.RequireAdmin;
import com.aifood.admin.service.TokenUsageService;
import com.ai.food.common.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理后台 Token 用量统计接口。
 *
 * <p>单一端点:
 * <ul>
 *   <li>GET /admin/api/token-usage/stats?groupBy=day|model|user&startDate=&endDate=</li>
 * </ul>
 *
 * <p>由 {@link com.aifood.admin.common.AdminWebConfig} 拦截器统一鉴权,
 * 类级 {@link RequireAdmin} 作为冗余防御。</p>
 */
@RestController
@RequestMapping("/admin/api/token-usage")
@RequireAdmin
@RequiredArgsConstructor
public class TokenUsageController {

    /** Token 用量聚合服务 */
    private final TokenUsageService service;

    /**
     * 聚合 token 用量。
     *
     * @param groupBy   分组维度,默认 day;支持 day / model / user(session)
     * @param startDate 起始日期 yyyy-MM-dd,可选
     * @param endDate   截止日期 yyyy-MM-dd,可选
     * @return 聚合结果数组,按 totalTokens 降序
     */
    @GetMapping("/stats")
    public ApiResponse<List<Map<String, Object>>> stats(
            @RequestParam(defaultValue = "day") String groupBy,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.success(service.stats(groupBy, startDate, endDate));
    }
}
