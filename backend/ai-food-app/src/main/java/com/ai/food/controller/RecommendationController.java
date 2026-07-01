package com.ai.food.controller;

import com.ai.food.common.mapper.RecommendationResultMapper;
import com.ai.food.common.model.RecommendationResult;
import com.ai.food.service.ai.AiService;
import com.ai.food.util.RateLimiterUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.benmanes.caffeine.cache.Cache;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RestController
@RequestMapping("/api/recommendation")
@RequiredArgsConstructor
@Tag(name = "推荐服务", description = "美食推荐相关接口，包括惯性模式、随机模式等")
public class RecommendationController {

    // [P0-AI-限流] inertia/random 走 LLM 推荐生成，按 userId 限流
    private static final int REC_RATE_LIMIT = 30;

    private final AiService aiService;

    @Qualifier("aiRateLimitCache")
    private final Cache<String, AtomicInteger> aiRateLimitCache;

    private final RecommendationResultMapper recommendationResultMapper;

    @PostMapping("/inertia")
    @Operation(summary = "惯性模式推荐", description = "基于用户历史偏好进行推荐，生成全新的美食建议")
    public ResponseEntity<RecommendationResponse> inertiaRecommend(
            @Parameter(description = "推荐请求参数", required = true)
            @RequestBody RecommendationRequest request) {

        Long userId = requireUserId();
        RateLimiterUtil.checkAndIncrement(aiRateLimitCache, "rec:inertia:" + userId, REC_RATE_LIMIT);

        log.debug("Inertia recommendation request - userId: {}, sessionId: {}", userId, request.getSessionId());

        StringBuilder paramsBuilder = new StringBuilder();
        if (request.getParams() != null) {
            request.getParams().forEach((key, value) ->
                paramsBuilder.append(key).append("：").append(value).append("\n")
            );
        }

        String aiResponse = aiService.generateRecommendation(paramsBuilder.toString());
        log.debug("AI response: {}", aiResponse);

        String foodName = parseFoodName(aiResponse);
        String reason = parseReason(aiResponse);

        RecommendationResponse response = new RecommendationResponse();
        response.setSessionId(request.getSessionId());
        response.setMode("inertia");
        response.setFoodName(foodName);
        response.setReason(reason);
        response.setSimilarityScore(null);
        response.setOldFood(null);
        response.setCreatedAt(LocalDateTime.now());

        log.debug("Inertia recommendation result: {}", response.getFoodName());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/random")
    @Operation(summary = "随机模式推荐", description = "随机获取历史记录与AI推荐，通过相似度判断选择最终推荐")
    public ResponseEntity<RecommendationResponse> randomRecommend(
            @Parameter(description = "推荐请求参数", required = true)
            @RequestBody RecommendationRequest request) {

        Long userId = requireUserId();
        RateLimiterUtil.checkAndIncrement(aiRateLimitCache, "rec:random:" + userId, REC_RATE_LIMIT);

        log.debug("Random recommendation request - userId: {}, sessionId: {}", userId, request.getSessionId());

        // Generate AI recommendation first
        StringBuilder paramsBuilder = new StringBuilder();
        if (request.getParams() != null) {
            request.getParams().forEach((key, value) ->
                paramsBuilder.append(key).append("：").append(value).append("\n")
            );
        }

        String aiResponse = aiService.generateRecommendation(paramsBuilder.toString());
        String newFood = parseFoodName(aiResponse);

        // ponytail: RecommendationResult 没有 userId 字段,查全表最近 5 条取最新一条作为旧值参考
        // (P2 再做按用户口味画像推荐)
        List<RecommendationResult> history = recommendationResultMapper.selectList(
                Wrappers.<RecommendationResult>lambdaQuery()
                        .orderByDesc(RecommendationResult::getCreatedAt)
                        .last("LIMIT 5")
        );
        String oldFood;
        if (!history.isEmpty()) {
            oldFood = history.get(0).getFoodName();
        } else {
            // 无历史时保留兜底,避免前端因 null 报错
            oldFood = "红烧肉";
        }

        double similarity = aiService.calculateSimilarity(oldFood, newFood);
        log.debug("Similarity between '{}' and '{}': {}", oldFood, newFood, similarity);

        boolean useNewFood = similarity >= 0.7;

        RecommendationResponse response = new RecommendationResponse();
        response.setSessionId(request.getSessionId());
        response.setMode("random");
        response.setFoodName(useNewFood ? newFood : oldFood);
        response.setOldFood(oldFood);
        response.setSimilarityScore(similarity);
        response.setReason(useNewFood
            ? parseReason(aiResponse)
            : "为您推荐了您常吃的" + oldFood + "，换换口味也不错哦");
        response.setCreatedAt(LocalDateTime.now());

        log.debug("Random recommendation result: useNewFood={}, foodName: {}", useNewFood, response.getFoodName());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/similarity")
    @Operation(summary = "计算相似度", description = "计算两种食物的相似度评分")
    public ResponseEntity<Map<String, Object>> calculateSimilarity(
            @Parameter(description = "食物A", required = true)
            @RequestParam String food1,
            @Parameter(description = "食物B", required = true)
            @RequestParam String food2) {

        Long userId = requireUserId();
        RateLimiterUtil.checkAndIncrement(aiRateLimitCache, "rec:similarity:" + userId, REC_RATE_LIMIT);

        log.debug("Calculate similarity request - userId: {}, food1: {}, food2: {}", userId, food1, food2);

        double similarity = aiService.calculateSimilarity(food1, food2);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "food1", food1,
                "food2", food2,
                "similarity", similarity,
                "threshold", 0.7,
                "isHighSimilarity", similarity >= 0.7
        ));
    }

    @GetMapping("/history/{sessionId}")
    @Operation(summary = "获取推荐历史", description = "获取指定会话的推荐历史记录")
    public ResponseEntity<Map<String, Object>> getRecommendationHistory(
            @Parameter(description = "会话ID", required = true)
            @PathVariable String sessionId) {

        log.debug("Get recommendation history for session: {}", sessionId);

        // ponytail: 复用 mapper 自带的 findBySessionId(带 LIMIT 1); 真要历史多条应加 findBySessionIdIn 或排序查询
        RecommendationResult result = recommendationResultMapper.findBySessionId(sessionId);

        List<Map<String, Object>> items = new ArrayList<>();
        if (result != null) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", result.getId());
            item.put("sessionId", result.getSessionId());
            item.put("mode", result.getMode());
            item.put("foodName", result.getFoodName());
            item.put("oldFood", result.getOldFood());
            item.put("similarityScore", result.getSimilarityScore());
            item.put("reason", result.getReason());
            item.put("createdAt", result.getCreatedAt());
            items.add(item);
        }

        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "recommendations", items
        ));
    }

    private String parseFoodName(String response) {
        if (response == null) return "未知美食";
        try {
            String json = extractJson(response);
            int start = json.indexOf("\"foodName\"");
            if (start >= 0) {
                start = json.indexOf(":", start) + 1;
                int end = json.indexOf(",", start);
                if (end < 0) end = json.indexOf("}", start);
                return json.substring(start, end).trim().replace("\"", "");
            }
        } catch (Exception e) {
            log.warn("Failed to parse foodName", e);
        }
        return "未知美食";
    }

    private String parseReason(String response) {
        if (response == null) return "根据您的需求为您推荐";
        try {
            String json = extractJson(response);
            int start = json.indexOf("\"reason\"");
            if (start >= 0) {
                start = json.indexOf(":", start) + 1;
                int end = json.lastIndexOf("}");
                return json.substring(start, end).trim().replace("\"", "");
            }
        } catch (Exception e) {
            log.warn("Failed to parse reason", e);
        }
        return "根据您的需求为您推荐";
    }

    private String extractJson(String response) {
        String trimmed = response.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('{');
            int end = trimmed.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return trimmed.substring(start, end + 1);
            }
        }
        if (trimmed.startsWith("{")) {
            return trimmed;
        }
        return "{}";
    }

    // ponytail: SecurityConfig 已要求 /api/recommendation/** 走 JWT；principal 为 String(userId)
    private Long requireUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("未登录");
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new RuntimeException("未登录");
        }
    }

    @Data
    public static class RecommendationRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "会话ID")
        private String sessionId;

        @io.swagger.v3.oas.annotations.media.Schema(description = "推荐模式", allowableValues = {"inertia", "random"})
        private String mode = "inertia";

        @io.swagger.v3.oas.annotations.media.Schema(description = "收集的参数")
        private Map<String, String> params;
    }

    @Data
    public static class RecommendationResponse {
        @io.swagger.v3.oas.annotations.media.Schema(description = "会话ID")
        private String sessionId;

        @io.swagger.v3.oas.annotations.media.Schema(description = "推荐模式")
        private String mode;

        @io.swagger.v3.oas.annotations.media.Schema(description = "推荐的美食名称")
        private String foodName;

        @io.swagger.v3.oas.annotations.media.Schema(description = "随机模式下的旧值")
        private String oldFood;

        @io.swagger.v3.oas.annotations.media.Schema(description = "相似度评分（随机模式）")
        private Double similarityScore;

        @io.swagger.v3.oas.annotations.media.Schema(description = "推荐理由")
        private String reason;

        @io.swagger.v3.oas.annotations.media.Schema(description = "创建时间")
        private LocalDateTime createdAt;
    }
}
