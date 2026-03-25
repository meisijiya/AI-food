package com.ai.food.controller;

import com.ai.food.model.ConversationSession;
import com.ai.food.model.CollectedParam;
import com.ai.food.model.QaRecord;
import com.ai.food.repository.ConversationSessionRepository;
import com.ai.food.repository.CollectedParamRepository;
import com.ai.food.repository.QaRecordRepository;
import com.ai.food.service.conversation.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/conversation")
@RequiredArgsConstructor
@Tag(name = "对话管理", description = "对话会话相关接口")
public class ConversationController {

    private final ConversationSessionRepository conversationSessionRepository;
    private final CollectedParamRepository collectedParamRepository;
    private final QaRecordRepository qaRecordRepository;
    private final ConversationService conversationService;

    @PostMapping("/start")
    @Operation(summary = "启动新的对话会话", description = "创建一个新的对话会话，返回sessionId和WebSocket连接地址")
    public ResponseEntity<StartConversationResponse> startConversation() {
        String sessionId = java.util.UUID.randomUUID().toString().replace("-", "");

        // 从 JWT 中获取当前用户 ID
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(auth.getPrincipal().toString());

        ConversationSession session = new ConversationSession();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setStatus("active");
        session.setMode("inertia");
        conversationSessionRepository.save(session);

        log.info("Conversation session created for user: {}", userId);

        StartConversationResponse response = new StartConversationResponse();
        response.setSessionId(sessionId);
        response.setWsUrl("/ws/conversation/" + sessionId);
        response.setCreatedAt(session.getCreatedAt());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{sessionId}")
    @Operation(summary = "获取会话状态", description = "获取指定会话的状态信息，包括已收集参数、当前阶段、进度等")
    public ResponseEntity<ConversationStatus> getConversationStatus(
            @Parameter(description = "会话ID", required = true)
            @PathVariable String sessionId) {
        Long userId = getCurrentUserId();
        conversationService.validateOwnership(sessionId, userId);

        var optSession = conversationSessionRepository.findBySessionId(sessionId);
        if (optSession.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ConversationSession session = optSession.get();
        List<CollectedParam> params = collectedParamRepository.findBySessionId(sessionId);

        Map<String, String> paramMap = params.stream()
                .collect(Collectors.toMap(CollectedParam::getParamName, CollectedParam::getParamValue, (a, b) -> a));

        ConversationStatus status = new ConversationStatus();
        status.setSessionId(sessionId);
        status.setStatus(session.getStatus());
        status.setCurrentQuestion(session.getCurrentQuestionCount());
        status.setTotalQuestions(session.getTotalQuestions());
        status.setCollectedParams(paramMap);

        return ResponseEntity.ok(status);
    }

    @PostMapping("/complete/{sessionId}")
    @Operation(summary = "手动结束对话", description = "手动结束收集参数，直接进入推荐阶段")
    public ResponseEntity<Map<String, Object>> completeConversation(
            @Parameter(description = "会话ID", required = true)
            @PathVariable String sessionId) {
        Long userId = getCurrentUserId();
        conversationService.validateOwnership(sessionId, userId);

        var optSession = conversationSessionRepository.findBySessionId(sessionId);
        if (optSession.isPresent()) {
            ConversationSession session = optSession.get();
            session.setStatus("completed");
            session.setCompletedAt(LocalDateTime.now());
            conversationSessionRepository.save(session);
        }

        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "status", "completed",
                "message", "已进入推荐阶段"
        ));
    }

    @DeleteMapping("/cancel/{sessionId}")
    @Operation(summary = "取消对话", description = "取消指定对话并删除所有关联数据")
    public ResponseEntity<Map<String, Object>> cancelConversation(
            @Parameter(description = "会话ID", required = true)
            @PathVariable String sessionId) {
        Long userId = getCurrentUserId();
        conversationService.validateOwnership(sessionId, userId);

        log.info("Canceling conversation via API");
        conversationService.cancelSession(sessionId);

        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "status", "canceled",
                "message", "对话已取消"
        ));
    }

    @GetMapping("/history/{sessionId}")
    @Operation(summary = "获取对话历史", description = "获取指定会话的问答记录列表")
    public ResponseEntity<Map<String, Object>> getConversationHistory(
            @Parameter(description = "会话ID", required = true)
            @PathVariable String sessionId) {
        Long userId = getCurrentUserId();
        conversationService.validateOwnership(sessionId, userId);

        List<QaRecord> records = qaRecordRepository.findBySessionIdOrderByQuestionOrderAsc(sessionId);

        List<Map<String, Object>> messages = records.stream().map(record -> {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("id", record.getId());
            msg.put("questionType", record.getQuestionType());
            msg.put("paramName", record.getParamName());
            msg.put("aiQuestion", record.getAiQuestion());
            msg.put("userAnswer", record.getUserAnswer());
            msg.put("isValid", record.getIsValid());
            msg.put("questionOrder", record.getQuestionOrder());
            msg.put("createdAt", record.getCreatedAt());
            return msg;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "messages", messages
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

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(authentication.getPrincipal().toString());
    }
}
