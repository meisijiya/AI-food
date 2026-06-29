package com.ai.food.controller;

import com.ai.food.common.util.ApiResponse;
import com.ai.food.service.ai.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI服务", description = "AI 聊天、回答验证、推荐生成、相似度计算（基于 Spring AI + DeepSeek）")
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    @Operation(summary = "AI 通用聊天", description = "发送 system prompt + 用户消息给 AI，返回 LLM 输出")
    public ResponseEntity<Map<String, Object>> chat(
            @Parameter(description = "系统提示词", required = true) @RequestParam String systemPrompt,
            @Parameter(description = "用户消息", required = true) @RequestParam String message) {

        log.debug("AI chat request - systemPrompt length: {}, message: {}",
                systemPrompt.length(), message);

        String response = aiService.chat(systemPrompt, message);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", message,
                "response", response
        ));
    }

    @PostMapping("/validate-answer")
    @Operation(summary = "验证用户回答", description = "用 LLM 判断用户对推荐问题的回答是否有效（JSON 格式）")
    public ResponseEntity<Map<String, Object>> validateAnswer(
            @Parameter(description = "参数类型（restriction/preference/health）", required = true) @RequestParam String param,
            @Parameter(description = "问题内容", required = true) @RequestParam String question,
            @Parameter(description = "用户回答", required = true) @RequestParam String answer) {
        boolean valid = aiService.validateAnswer(param, question, answer);
        return ResponseEntity.ok(Map.of(
                "valid", valid
        ));
    }

    @PostMapping("/recommend")
    @Operation(summary = "生成美食推荐", description = "基于收集到的 7 个参数（时间/地点/天气/心情/同行者/预算/口味）生成美食推荐（JSON 格式）")
    public ResponseEntity<Map<String, Object>> recommend(
            @Parameter(description = "已收集参数的 JSON 字符串", required = true) @RequestParam String collectedParams) {
        String result = aiService.generateRecommendation(collectedParams);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "recommendation", result
        ));
    }

    @PostMapping("/similarity")
    @Operation(summary = "计算食物相似度", description = "用 LLM 评估两种食物的相似度（0-1 浮点数）")
    public ResponseEntity<Map<String, Object>> similarity(
            @Parameter(description = "食物 A 名称", required = true) @RequestParam String food1,
            @Parameter(description = "食物 B 名称", required = true) @RequestParam String food2) {
        double score = aiService.calculateSimilarity(food1, food2);
        return ResponseEntity.ok(Map.of(
                "food1", food1,
                "food2", food2,
                "similarity", score
        ));
    }
}
