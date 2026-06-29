package com.aifood.admin.controller;

import com.ai.food.common.util.ApiResponse;
import com.aifood.admin.common.annotation.RequireAdmin;
import com.aifood.admin.dto.DashboardSummaryVO;
import com.aifood.admin.dto.TrendVO;
import com.aifood.admin.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理后台 Dashboard 接口。
 *
 * <p>两个端点:
 * <ul>
 *   <li>GET /admin/api/dashboard/summary       首页聚合指标</li>
 *   <li>GET /admin/api/dashboard/trends?days=N  最近 N 天趋势(默认 7)</li>
 * </ul>
 *
 * <p>由 {@link com.aifood.admin.common.AdminWebConfig} 拦截器统一鉴权,
 * 类级 {@link RequireAdmin} 作为冗余防御。</p>
 */
@RestController
@RequestMapping("/admin/api/dashboard")
@RequireAdmin
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /** 首页汇总指标 */
    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryVO> summary() {
        return ApiResponse.success(dashboardService.summary());
    }

    /** 最近 N 天趋势,days 默认 7 */
    @GetMapping("/trends")
    public ApiResponse<Map<String, List<TrendVO>>> trends(
            @RequestParam(defaultValue = "7") int days) {
        return ApiResponse.success(dashboardService.trends(days));
    }
}
