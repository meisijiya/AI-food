package com.ai.food.service.conversation;

import com.ai.food.dto.ConversationState;
import com.ai.food.dto.WebSocketMessage;
import com.ai.food.model.CollectedParam;
import com.ai.food.model.ConversationSession;
import com.ai.food.model.QaRecord;
import com.ai.food.model.RecommendationResult;
import com.ai.food.repository.CollectedParamRepository;
import com.ai.food.repository.ConversationSessionRepository;
import com.ai.food.repository.QaRecordRepository;
import com.ai.food.repository.RecommendationResultRepository;
import com.ai.food.service.ai.AiService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ai.food.validator.MessageValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AiService aiService;
    private final MessageValidator messageValidator;
    private final MessageTagParser messageTagParser;
    private final ConversationSessionRepository conversationSessionRepository;
    private final QaRecordRepository qaRecordRepository;
    private final CollectedParamRepository collectedParamRepository;
    private final RecommendationResultRepository recommendationResultRepository;
    private final StringRedisTemplate redisTemplate;

    @Value("${ai.conversation.min-questions:7}")
    private int minQuestions;

    @Value("${ai.conversation.max-questions:10}")
    private int maxQuestions;

    @Value("${ai.conversation.max-params-retry:2}")
    private int maxParamRetry;

    private static final int REQUIRED_PARAMS_COUNT = 7;

    private final List<String> requiredParams = Arrays.asList(
        "time", "location", "weather", "mood", "companion", "budget", "taste"
    );

    private final List<String> optionalParams = Arrays.asList(
        "restriction", "preference", "health"
    );

    // ==================== 初始化 ====================

    public ConversationState initializeConversation(String sessionId) {
        int effectiveMin = Math.max(minQuestions, REQUIRED_PARAMS_COUNT);
        int effectiveMax = Math.max(maxQuestions, effectiveMin);
        int totalQuestions = new Random().nextInt(effectiveMax - effectiveMin + 1) + effectiveMin;

        log.info("Initializing session {} with {} total questions", sessionId, totalQuestions);

        ConversationState state = new ConversationState(sessionId, totalQuestions, "inertia");
        state.setCurrentParam("time");

        conversationSessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            session.setTotalQuestions(totalQuestions);
            conversationSessionRepository.save(session);
        });

        return state;
    }

    public WebSocketMessage getFirstQuestion(ConversationState state) {
        state.incrementQuestionCount();
        state.setCurrentParam("time");
        updateSessionQuestionCount(state);
        return createMessage("question", "time",
                messageTagParser.getQuestionContent("time", state), state);
    }

    // ==================== 核心：处理用户回答 ====================
    // 返回 List：第一条 = AI 确认/追问，第二条(可选) = 下一个问题

    public List<WebSocketMessage> processAnswer(String sessionId, String answer, ConversationState state) {
        log.info("[{}] processAnswer: '{}' (param={}, q={}/{})",
                sessionId, answer, state.getCurrentParam(),
                state.getCurrentQuestionCount(), state.getTotalQuestions());

        List<WebSocketMessage> result = new ArrayList<>();
        String currentParam = state.getCurrentParam();

        // 1. 关键词校验
        MessageValidator.ValidationResult validation = messageValidator.validate(currentParam, answer);

        if (!validation.isValid()) {
            state.incrementParamRetry(currentParam);
            if (state.canRetryParam(currentParam, maxParamRetry)) {
                // 追问
                saveQaRecord(sessionId, "2question", currentParam,
                        messageTagParser.getQuestionContent(currentParam, state), answer, false,
                        state.getCurrentQuestionCount());
                result.add(createMessage("2question", currentParam,
                        messageTagParser.getRetryContent(currentParam, validation.getMessage()), state));
                return result;
            } else {
                // 超过重试次数，跳过该参数
                log.warn("Param {} exceeded max retry, skipping", currentParam);
                state.saveParamValue(currentParam, "未提供");
                saveCollectedParam(sessionId, currentParam, "未提供");
            }
        } else {
            // 校验通过，保存参数
            state.saveParamValue(currentParam, answer);
            saveQaRecord(sessionId, "question", currentParam,
                    messageTagParser.getQuestionContent(currentParam, state), answer, true,
                    state.getCurrentQuestionCount());
            saveCollectedParam(sessionId, currentParam, answer);
        }

        // 2. 检查是否进入自由发挥阶段
        if (state.getCurrentQuestionCount() >= REQUIRED_PARAMS_COUNT && !state.isInFreeFormStage()) {
            state.enterFreeFormStage();
            log.info("[{}] entered free form stage", sessionId);
        }

        // 3. 调用 AI 生成个性化确认/闲聊消息
        String aiResponse = generateAiResponse(currentParam, answer, state);
        result.add(createMessage("chat", currentParam, aiResponse, state));

        // 4. 确定下一个问题或进入推荐
        if (state.isCompleted() || allParamsCollected(state)) {
            result.add(generateRecommendationMessage(sessionId, state));
            return result;
        }

        String nextParam = determineNextParam(state);
        if (nextParam == null) {
            result.add(generateRecommendationMessage(sessionId, state));
            return result;
        }

        // 推进到下一个参数
        state.incrementQuestionCount();
        state.setCurrentParam(nextParam);
        updateSessionQuestionCount(state);

        String nextQuestion = messageTagParser.getQuestionContent(nextParam, state);
        result.add(createMessage("question", nextParam, nextQuestion, state));

        return result;
    }

    // ==================== 处理打断消息 ====================

    public WebSocketMessage handleInterrupt(String combinedMessage, ConversationState state) {
        log.info("[{}] handleInterrupt: '{}'", state.getSessionId(), combinedMessage);

        // 用 AI 统一回复多条打断消息
        String context = buildParamsContext(state);
        String prompt = String.format(
            "用户刚才快速发了几条消息：「%s」。已收集信息：%s。" +
            "请用一句轻松的话回应，比如「你说话太快啦」或者直接汇总理解用户的意思。15字以内。",
            combinedMessage, context
        );

        String aiReply;
        try {
            aiReply = aiService.chat("你是一个友好的美食推荐助手。", prompt);
            if (aiReply == null || aiReply.isBlank() || aiReply.startsWith("抱歉")) {
                aiReply = "你说话太快啦，让我先想想~";
            }
        } catch (Exception e) {
            aiReply = "你说话太快啦，让我先想想~";
        }

        return createMessage("interrupt", null, aiReply, state);
    }

    // ==================== 推荐 ====================

    public WebSocketMessage generateRecommendationMessage(String sessionId, ConversationState state) {
        StringBuilder paramsSummary = new StringBuilder();
        state.getParamValues().forEach((key, value) ->
            paramsSummary.append(messageTagParser.getParamDisplayName(key))
                    .append(": ").append(value).append("\n")
        );

        log.info("[{}] generating recommendation with params:\n{}", sessionId, paramsSummary);

        String aiResponse;
        try {
            aiResponse = aiService.generateRecommendation(paramsSummary.toString());
        } catch (Exception e) {
            log.error("AI recommendation failed", e);
            aiResponse = null;
        }

        String normalizedJson = extractJsonFromResponse(aiResponse);
        saveRecommendationResult(sessionId, state, normalizedJson);

        try {
            Long userId = getUserIdFromSession(sessionId);
            String cacheKey = "pending:recommend:" + userId;
            Map<String, Object> cacheData = new LinkedHashMap<>();
            cacheData.put("sessionId", sessionId);
            cacheData.put("foodName", parseFoodName(normalizedJson));
            cacheData.put("reason", parseReason(normalizedJson));
            cacheData.put("paramValues", state.getParamValues());
            cacheData.put("photoUploaded", false);
            cacheData.put("photoUrl", null);
            cacheData.put("createdAt", LocalDateTime.now().toString());
            redisTemplate.opsForValue().set(cacheKey, OBJECT_MAPPER.writeValueAsString(cacheData), 7, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("Failed to cache pending recommendation to Redis for session {}", sessionId, e);
        }

        conversationSessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            session.setStatus("completed");
            session.setCompletedAt(LocalDateTime.now());
            conversationSessionRepository.save(session);
        });

        WebSocketMessage msg = new WebSocketMessage();
        msg.setType("recommend");
        msg.setContent(normalizedJson);
        msg.setProgress(createProgress(state));
        return msg;
    }

    // ==================== AI 调用 ====================

    private String generateAiResponse(String param, String answer, ConversationState state) {
        String displayName = messageTagParser.getParamDisplayName(param);
        String context = buildParamsContext(state);

        // 如果所有必选参数已收集完，让 AI 自由对话
        boolean allRequiredDone = requiredParams.stream().allMatch(p ->
                p.equals(param) || state.isParamCollected(p));

        String systemPrompt;
        String userPrompt;

        if (allRequiredDone && state.isInFreeFormStage()) {
            systemPrompt = "你是一个友好的美食推荐助手。用一句自然的中文回应用户，可以适当追问饮食偏好或禁忌。20字以内。";
            userPrompt = String.format("用户说「%s」。已收集信息：%s。请自然地回应。", answer, context);
        } else {
            systemPrompt = "你是一个友好的美食推荐助手。用一句简短自然的中文确认用户的信息，不要重复用户原话。15字以内。";
            userPrompt = String.format(
                "用户回答了关于「%s」的问题：「%s」。已收集：%s。请确认。",
                displayName, answer, context
            );
        }

        try {
            String response = aiService.chat(systemPrompt, userPrompt);
            if (response != null && !response.isBlank() && !response.startsWith("抱歉")) {
                return response.trim();
            }
        } catch (Exception e) {
            log.warn("AI response generation failed: {}", e.getMessage());
        }
        // fallback
        return String.format("好的，%s我记下了！", answer);
    }

    // ==================== 工具方法 ====================

    private boolean allParamsCollected(ConversationState state) {
        return requiredParams.stream().allMatch(state::isParamCollected)
                && optionalParams.stream().allMatch(state::isParamCollected);
    }

    private String determineNextParam(ConversationState state) {
        for (String param : requiredParams) {
            if (!state.isParamCollected(param)) return param;
        }
        if (state.isInFreeFormStage()) {
            for (String param : optionalParams) {
                if (!state.isParamCollected(param)) return param;
            }
        }
        return null;
    }

    private String buildParamsContext(ConversationState state) {
        StringBuilder sb = new StringBuilder();
        state.getParamValues().forEach((key, value) ->
            sb.append(messageTagParser.getParamDisplayName(key)).append(": ").append(value).append("；")
        );
        return sb.toString();
    }

    private String extractJsonFromResponse(String response) {
        if (response == null) return "{\"foodName\":\"暂无推荐\",\"reason\":\"无法生成推荐\"}";
        String trimmed = response.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('{');
            int end = trimmed.lastIndexOf('}');
            if (start >= 0 && end > start) return trimmed.substring(start, end + 1);
        }
        if (trimmed.startsWith("{")) return trimmed;
        return "{\"foodName\":\"" + trimmed.replace("\"", "\\\"") + "\",\"reason\":\"根据您的需求为您推荐\"}";
    }

    private void saveRecommendationResult(String sessionId, ConversationState state, String recommendationJson) {
        try {
            RecommendationResult result = recommendationResultRepository.findBySessionId(sessionId)
                    .orElseGet(RecommendationResult::new);
            Map<String, String> payload = parseRecommendationPayload(recommendationJson);
            result.setSessionId(sessionId);
            result.setMode(state.getMode());
            result.setFoodName(payload.getOrDefault("foodName", "暂无推荐结果"));
            result.setReason(payload.getOrDefault("reason", "该会话暂无可展示的推荐说明"));
            RecommendationResult saved = recommendationResultRepository.saveAndFlush(result);
            log.info("Saved recommendation result: sessionId={}, foodName={}", sessionId, saved.getFoodName());
        } catch (Exception e) {
            log.error("Failed to save recommendation result for session {}", sessionId, e);
        }
    }

    private Map<String, String> parseRecommendationPayload(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse recommendation json, using fallback text: {}", json);
            return Map.of("foodName", json, "reason", "根据您的需求为您推荐");
        }
    }

    private WebSocketMessage createMessage(String type, String param, String content, ConversationState state) {
        WebSocketMessage msg = new WebSocketMessage();
        msg.setType(type);
        msg.setParam(param);
        msg.setContent(content);
        msg.setProgress(createProgress(state));
        return msg;
    }

    private WebSocketMessage.Progress createProgress(ConversationState state) {
        return new WebSocketMessage.Progress(
                state.getCurrentQuestionCount(),
                state.getTotalQuestions(),
                state.getCollectedParams()
        );
    }

    private void saveQaRecord(String sessionId, String questionType, String paramName,
                              String aiQuestion, String userAnswer, boolean isValid, int questionOrder) {
        try {
            QaRecord record = new QaRecord();
            record.setSessionId(sessionId);
            record.setQuestionType(questionType);
            record.setParamName(paramName);
            record.setAiQuestion(aiQuestion);
            record.setUserAnswer(userAnswer);
            record.setIsValid(isValid);
            record.setQuestionOrder(questionOrder);
            qaRecordRepository.save(record);
        } catch (Exception e) {
            log.error("Failed to save QaRecord", e);
        }
    }

    private void saveCollectedParam(String sessionId, String paramName, String paramValue) {
        try {
            var existing = collectedParamRepository.findBySessionIdAndParamName(sessionId, paramName);
            CollectedParam param = existing.orElseGet(() -> {
                CollectedParam p = new CollectedParam();
                p.setSessionId(sessionId);
                p.setParamName(paramName);
                p.setParamType(requiredParams.contains(paramName) ? "required" : "optional");
                return p;
            });
            param.setParamValue(paramValue);
            collectedParamRepository.save(param);
        } catch (Exception e) {
            log.error("Failed to save CollectedParam", e);
        }
    }

    private void updateSessionQuestionCount(ConversationState state) {
        try {
            conversationSessionRepository.findBySessionId(state.getSessionId()).ifPresent(session -> {
                session.setCurrentQuestionCount(state.getCurrentQuestionCount());
                conversationSessionRepository.save(session);
            });
        } catch (Exception e) {
            log.error("Failed to update session question count", e);
        }
    }

    public List<String> getRequiredParams() { return requiredParams; }
    public List<String> getOptionalParams() { return optionalParams; }

    private Long getUserIdFromSession(String sessionId) {
        return conversationSessionRepository.findBySessionId(sessionId)
                .map(ConversationSession::getUserId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
    }

    private String parseFoodName(String response) {
        if (response == null) return "未知美食";
        try {
            String trimmed = response.trim();
            if (trimmed.startsWith("```")) {
                int start = trimmed.indexOf('{');
                int end = trimmed.lastIndexOf('}');
                if (start >= 0 && end > start) trimmed = trimmed.substring(start, end + 1);
            }
            int start = trimmed.indexOf("\"foodName\"");
            if (start >= 0) {
                start = trimmed.indexOf(":", start) + 1;
                int end = trimmed.indexOf(",", start);
                if (end < 0) end = trimmed.indexOf("}", start);
                return trimmed.substring(start, end).trim().replace("\"", "");
            }
        } catch (Exception e) {
            log.warn("Failed to parse foodName", e);
        }
        return "未知美食";
    }

    private String parseReason(String response) {
        if (response == null) return "根据您的需求为您推荐";
        try {
            String trimmed = response.trim();
            if (trimmed.startsWith("```")) {
                int start = trimmed.indexOf('{');
                int end = trimmed.lastIndexOf('}');
                if (start >= 0 && end > start) trimmed = trimmed.substring(start, end + 1);
            }
            int start = trimmed.indexOf("\"reason\"");
            if (start >= 0) {
                start = trimmed.indexOf(":", start) + 1;
                int end = trimmed.lastIndexOf("}");
                return trimmed.substring(start, end).trim().replace("\"", "");
            }
        } catch (Exception e) {
            log.warn("Failed to parse reason", e);
        }
        return "根据您的需求为您推荐";
    }

    public boolean isAllRequiredParamsCollected(ConversationState state) {
        return requiredParams.stream().allMatch(state::isParamCollected);
    }

    public int getRemainingRequiredParams(ConversationState state) {
        return (int) requiredParams.stream().filter(p -> !state.isParamCollected(p)).count();
    }

    @Transactional
    public void cancelSession(String sessionId) {
        log.info("[{}] canceling session - soft deleting all related data", sessionId);
        try {
            qaRecordRepository.softDeleteBySessionId(sessionId);
            collectedParamRepository.softDeleteBySessionId(sessionId);
            recommendationResultRepository.softDeleteBySessionId(sessionId);
            conversationSessionRepository.softDeleteBySessionId(sessionId);
            log.info("[{}] session soft deleted", sessionId);
        } catch (Exception e) {
            log.error("[{}] failed to cancel session", sessionId, e);
        }
    }
}
