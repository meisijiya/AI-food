package com.aifood.admin.controller;

import com.ai.food.common.model.RecommendationResult;
import com.ai.food.common.mapper.RecommendationResultMapper;
import com.ai.food.common.util.ApiResponse;
import com.aifood.admin.common.annotation.RequireAdmin;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台推荐结果查询接口。
 *
 * <p>单一端点:
 * <ul>
 *   <li>GET /admin/api/recommendations?page=&size=&sessionId=&mode=</li>
 * </ul>
 *
 * <p>{@code sessionId} / {@code mode} 可选,用于按会话或模式过滤;
 * 实体未包含 userId/accepted 字段(由 session 维度间接表达),不提供对应筛选。</p>
 *
 * <p>由 {@link com.aifood.admin.common.AdminWebConfig} 拦截器统一鉴权,
 * 类级 {@link RequireAdmin} 作为冗余防御。</p>
 */
@RestController
@RequestMapping("/admin/api/recommendations")
@RequireAdmin
@RequiredArgsConstructor
public class RecommendationController {

    /** 推荐结果 MyBatis-Plus Mapper */
    private final RecommendationResultMapper recommendationMapper;

    /**
     * 分页查询推荐结果,按 created_at 倒序。
     *
     * @param page      页码,默认 1
     * @param size      页大小,默认 20
     * @param sessionId 会话 id 可选过滤(RecommendationResult 用 sessionId 关联而非 userId)
     * @param mode      模式(random / similarity 等)可选过滤
     * @param foodName  食物名模糊匹配可选过滤
     * @return 分页结果
     */
    @GetMapping
    public ApiResponse<Page<RecommendationResult>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) String foodName) {
        Page<RecommendationResult> p = new Page<>(page, size);
        LambdaQueryWrapper<RecommendationResult> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(sessionId)) w.eq(RecommendationResult::getSessionId, sessionId);
        if (StringUtils.hasText(mode)) w.eq(RecommendationResult::getMode, mode);
        if (StringUtils.hasText(foodName)) w.like(RecommendationResult::getFoodName, foodName);
        w.orderByDesc(RecommendationResult::getCreatedAt);
        return ApiResponse.success(recommendationMapper.selectPage(p, w));
    }
}
