package com.ai.food.controller;

import com.ai.food.model.ConversationSession;
import com.ai.food.model.CollectedParam;
import com.ai.food.model.QaRecord;
import com.ai.food.repository.ConversationSessionRepository;
import com.ai.food.repository.CollectedParamRepository;
import com.ai.food.repository.QaRecordRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/start")
    @Operation(summary = "启动新的对话会话", description = "创建一个新的对话会话，返回sessionId和WebSocket连接地址")
    public ResponseEntity<StartConversationResponse> startConversation() {
        String sessionId = java.util.UUID.randomUUID().toString().replace("-", "");

        ConversationSession session = new ConversationSession();
        session.setSessionId(sessionId);
        session.setStatus("active");
        session.setMode("inertia");
        conversationSessionRepository.save(session);

        log.info("Conversation session created: {}", sessionId);

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

    @GetMapping("/history/{sessionId}")
    @Operation(summary = "获取对话历史", description = "获取指定会话的问答记录列表")
    public ResponseEntity<Map<String, Object>> getConversationHistory(
            @Parameter(description = "会话ID", required = true)
            @PathVariable String sessionId) {

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
}
