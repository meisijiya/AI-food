package com.ai.food.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/conversation")
@RequiredArgsConstructor
@Tag(name = "对话管理", description = "对话会话相关接口")
public class ConversationController {

    @PostMapping("/start")
    @Operation(summary = "启动新的对话会话", description = "创建一个新的对话会话，返回sessionId和WebSocket连接地址")
    public ResponseEntity<StartConversationResponse> startConversation() {
        String sessionId = java.util.UUID.randomUUID().toString().replace("-", "");
        
        StartConversationResponse response = new StartConversationResponse();
        response.setSessionId(sessionId);
        response.setWsUrl("/ws/conversation/" + sessionId);
        response.setCreatedAt(LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{sessionId}")
    @Operation(summary = "获取会话状态", description = "获取指定会话的状态信息，包括已收集参数、当前阶段、进度等")
    public ResponseEntity<ConversationStatus> getConversationStatus(
            @Parameter(description = "会话ID", required = true)
            @PathVariable String sessionId) {
        
        ConversationStatus status = new ConversationStatus();
        status.setSessionId(sessionId);
        status.setStatus("active");
        status.setCurrentQuestion(3);
        status.setTotalQuestions(7);
        status.setCollectedParams(Map.of(
                "time", "晚上",
                "location", "公司",
                "weather", "晴天"
        ));
        
        return ResponseEntity.ok(status);
    }

    @PostMapping("/complete/{sessionId}")
    @Operation(summary = "手动结束对话", description = "手动结束收集参数，直接进入推荐阶段")
    public ResponseEntity<Map<String, Object>> completeConversation(
            @Parameter(description = "会话ID", required = true)
            @PathVariable String sessionId) {
        
        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "status", "completed",
                "message", "已进入推荐阶段"
        ));
    }

    @GetMapping("/history/{sessionId}")
    @Operation(summary = "获取对话历史", description = "获取指定会话的问答记录列表")
    public ResponseEntity<Map<String, Object>> getConversationHistory(
            @Parameter(description = "会话ID", required = true)
            @PathVariable String sessionId) {
        
        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "messages", java.util.List.of()
        ));
    }

    @Data
    static class StartConversationResponse {
        @io.swagger.v3.oas.annotations.media.Schema(description = "会话ID")
        private String sessionId;
        
        @io.swagger.v3.oas.annotations.media.Schema(description = "WebSocket连接地址")
        private String wsUrl;
        
        @io.swagger.v3.oas.annotations.media.Schema(description = "创建时间")
        private LocalDateTime createdAt;
    }

    @Data
    static class ConversationStatus {
        @io.swagger.v3.oas.annotations.media.Schema(description = "会话ID")
        private String sessionId;
        
        @io.swagger.v3.oas.annotations.media.Schema(description = "会话状态", allowableValues = {"active", "completed", "cancelled"})
        private String status;
        
        @io.swagger.v3.oas.annotations.media.Schema(description = "当前问题序号")
        private Integer currentQuestion;
        
        @io.swagger.v3.oas.annotations.media.Schema(description = "总问题数")
        private Integer totalQuestions;
        
        @io.swagger.v3.oas.annotations.media.Schema(description = "已收集的参数")
        private Map<String, String> collectedParams;
    }
}