package com.ai.food.controller;

import com.ai.food.service.ai.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/recommendation")
@RequiredArgsConstructor
@Tag(name = "推荐服务", description = "美食推荐相关接口，包括惯性模式、随机模式等")
public class RecommendationController {

    private final AiService aiService;

    @PostMapping("/inertia")
    @Operation(summary = "惯性模式推荐", description = "基于用户历史偏好进行推荐，生成全新的美食建议")
    public ResponseEntity<RecommendationResponse> inertiaRecommend(
            @Parameter(description = "推荐请求参数", required = true)
            @RequestBody RecommendationRequest request) {

        log.debug("Inertia recommendation request - sessionId: {}", request.getSessionId());

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

        log.debug("Random recommendation request - sessionId: {}", request.getSessionId());

        // Generate AI recommendation first
        StringBuilder paramsBuilder = new StringBuilder();
        if (request.getParams() != null) {
            request.getParams().forEach((key, value) ->
                paramsBuilder.append(key).append("：").append(value).append("\n")
            );
        }

        String aiResponse = aiService.generateRecommendation(paramsBuilder.toString());
        String newFood = parseFoodName(aiResponse);

        // For random mode, use a placeholder old food (would come from RagFlow in full implementation)
        String oldFood = "红烧肉";

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

        log.debug("Calculate similarity request - food1: {}, food2: {}", food1, food2);

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

        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "recommendations", java.util.List.of()
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
