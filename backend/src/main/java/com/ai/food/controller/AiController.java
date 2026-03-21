package com.ai.food.controller;

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
@Tag(name = "AI服务", description = "AI服务相关接口，包括聊天、问答验证、推荐生成等")
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    @Operation(summary = "AI聊天", description = "发送消息给AI并获取回复")
    public ResponseEntity<Map<String, Object>> chat(
            @Parameter(description = "系统提示词", required = true)
            @RequestParam String systemPrompt,
            @Parameter(description = "用户消息", required = true)
            @RequestParam String message) {

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
    @Operation(summary = "验证回答", description = "验证用户回答是否有效")
    public ResponseEntity<Map<String, Object>> validateAnswer(
            @Parameter(description = "参数类型", required = true)
            @RequestParam String param,
            @Parameter(description = "问题内容", required = true)
            @RequestParam String question,
            @Parameter(description = "用户回答", required = true)
            @RequestParam String answer) {

        log.debug("Validate answer request - param: {}, question: {}, answer: {}", 
                param, question, answer);

        boolean isValid = aiService.validateAnswer(param, question, answer);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "param", param,
                "answer", answer,
                "isValid", isValid
        ));
    }

    @PostMapping("/generate-question")
    @Operation(summary = "生成问题", description = "根据参数类型生成问题")
    public ResponseEntity<Map<String, Object>> generateQuestion(
            @Parameter(description = "参数类型", required = true)
            @RequestParam String param,
            @Parameter(description = "上下文信息")
            @RequestParam(required = false, defaultValue = "") String context) {

        log.debug("Generate question request - param: {}, context: {}", param, context);

        String question = aiService.generateQuestion(param, context);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "param", param,
                "question", question
        ));
    }

    @PostMapping("/recommend")
    @Operation(summary = "生成推荐", description = "根据收集的参数生成美食推荐")
    public ResponseEntity<Map<String, Object>> recommend(
            @Parameter(description = "收集的参数信息", required = true)
            @RequestBody String collectedParams) {

        log.debug("Generate recommendation request - params length: {}", collectedParams.length());

        String recommendation = aiService.generateRecommendation(collectedParams);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "params", collectedParams,
                "recommendation", recommendation
        ));
    }

    @GetMapping("/similarity")
    @Operation(summary = "计算相似度", description = "计算两种食物的相似度")
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
                "isHighSimilarity", similarity >= 0.7
        ));
    }

    @GetMapping("/test")
    @Operation(summary = "测试AI连接", description = "测试DeepSeek API连接是否正常")
    public ResponseEntity<Map<String, Object>> testConnection(
            @Parameter(description = "测试消息")
            @RequestParam(defaultValue = "你好，请介绍一下你自己") String message) {

        log.debug("Test connection request - message: {}", message);

        String response = aiService.chat("你是一个美食推荐助手", message);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", message,
                "response", response
        ));
    }

    @GetMapping("/test-recommend")
    @Operation(summary = "测试推荐", description = "使用预设参数测试美食推荐功能")
    public ResponseEntity<Map<String, Object>> testRecommend() {
        log.debug("Test recommend request");

        String params = """
            时间：晚上7点
            地点：北京
            天气：晴天
            心情：放松
            同行人：朋友
            预算：100-200元
            口味偏好：辣
            """;

        String response = aiService.generateRecommendation(params);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "params", params,
                "recommendation", response
        ));
    }
}
