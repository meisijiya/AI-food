package com.ai.food.service.conversation;

import com.ai.food.dto.ConversationState;
import com.ai.food.dto.WebSocketMessage;
import com.ai.food.mapper.CollectedParamMapper;
import com.ai.food.mapper.ConversationSessionMapper;
import com.ai.food.mapper.QaRecordMapper;
import com.ai.food.mapper.RecommendationResultMapper;
import com.ai.food.model.CollectedParam;
import com.ai.food.model.ConversationSession;
import com.ai.food.model.QaRecord;
import com.ai.food.model.RecommendationResult;
import com.ai.food.service.ai.AiService;
import com.ai.food.service.bloom.BloomFilterService;
import com.ai.food.service.match.ParamNormalizationService;
import com.ai.food.service.conversation.MessageTagParser;
import com.ai.food.validator.MessageValidator;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 对话会话业务服务（MyBatis-Plus 迁移版）。
 * <p>
 * 继承 {@link ServiceImpl} 后，{@code baseMapper} 指向 {@link ConversationSessionMapper}；
 * 其余三张表（QA / 已收集参数 / 推荐结果）走注入的 Mapper 字段。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService extends ServiceImpl<ConversationSessionMapper, ConversationSession> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AiService aiService;
    private final MessageValidator messageValidator;
    private final MessageTagParser messageTagParser;
    private final QaRecordMapper qaRecordMapper;
    private final CollectedParamMapper collectedParamMapper;
    private final RecommendationResultMapper recommendationResultMapper;
    private final StringRedisTemplate redisTemplate;
    private final BloomFilterService bloomFilterService;
    private final ParamNormalizationService paramNormalizationService;

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

    // ==================== 权限校验 ====================

    /**
     * 校验 session 是否属于当前用户，并拒绝读取超过 30 天的已完成会话。
     */
    public void validateOwnership(String sessionId, Long userId) {
        ConversationSession session = baseMapper.findBySessionId(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }
        if (!userId.equals(session.getUserId())) {
            throw new RuntimeException("无权访问此会话");
        }
        // 已完成超过 30 天的会话不可读取
        if ("completed".equals(session.getStatus()) && session.getCompletedAt() != null
                && session.getCompletedAt().isBefore(LocalDateTime.now().minusDays(30))) {
            throw new RuntimeException("会话已过期");
        }
    }

    // ==================== 初始化 ====================

    /**
     * 初始化会话：随机决定总题数，并（若已存在）刷新 totalQuestions 字段。
     */
    public ConversationState initializeConversation(String sessionId) {
        int effectiveMin = Math.max(minQuestions, REQUIRED_PARAMS_COUNT);
        int effectiveMax = Math.max(maxQuestions, effectiveMin);
        int totalQuestions = new Random().nextInt(effectiveMax - effectiveMin + 1) + effectiveMin;

        log.debug("Initializing session with {} total questions", totalQuestions);

        ConversationState state = new ConversationState(sessionId, totalQuestions, "inertia");
        state.setCurrentParam("time");

        ConversationSession session = baseMapper.findBySessionId(sessionId);
        if (session != null) {
            session.setTotalQuestions(totalQuestions);
            baseMapper.updateById(session);
        }

        return state;
    }

    /**
     * 推第一道题（time 参数），并把题号 +1 持久化。
     */
    public WebSocketMessage getFirstQuestion(ConversationState state) {
        state.incrementQuestionCount();
        state.setCurrentParam("time");
        updateSessionQuestionCount(state);
        return createMessage("question", "time",
                messageTagParser.getQuestionContent("time", state), state);
    }

    // ==================== 核心：处理用户回答 ====================
    // 返回 List：第一条 = AI 确认/追问，第二条(可选) = 下一个问题

    /**
     * 处理一轮用户回答：参数校验 / 重试逻辑 / AI 确认 / 推进 / 推荐触发。
     */
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

    /**
     * 用户连续发送多条消息时，使用 AI 统一回复"你说话太快啦"等打断响应。
     */
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

    /**
     * 触发推荐：调用 AI 生成结果 → 持久化 → 写 Bloom 画像 → 标记会话完成 → 缓存 pending。
     */
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
        boolean saved = saveRecommendationResult(sessionId, state, normalizedJson);

        if (!saved) {
            WebSocketMessage msg = new WebSocketMessage();
            msg.setType("error");
            msg.setContent("推荐结果保存失败，请稍后重试");
            msg.setProgress(createProgress(state));
            return msg;
        }

        try {
            Long userId = getUserIdFromSession(sessionId);
            String cacheKey = "pending:recommend:" + userId;
            redisTemplate.opsForValue().set(cacheKey, sessionId, 7, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("Failed to cache pending recommendation to Redis for session {}", sessionId, e);
        }

        ConversationSession session = baseMapper.findBySessionId(sessionId);
        if (session != null) {
            session.setStatus("completed");
            session.setCompletedAt(LocalDateTime.now());
            baseMapper.updateById(session);
        }

        WebSocketMessage msg = new WebSocketMessage();
        msg.setType("recommend");
        msg.setContent(normalizedJson);
        msg.setProgress(createProgress(state));
        return msg;
    }

    // ==================== AI 调用 ====================

    /**
     * 根据当前收集进度切换 prompt：必选参数未收齐走"简短确认"，收齐后走"自由对话"prompt。
     */
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

    /**
     * 是否所有必选 + 可选参数都已收集。
     */
    private boolean allParamsCollected(ConversationState state) {
        return requiredParams.stream().allMatch(state::isParamCollected)
                && optionalParams.stream().allMatch(state::isParamCollected);
    }

    /**
     * 决定下一个待收集的参数（必选优先，自由发挥阶段后补可选参数）。
     */
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

    /**
     * 把已收集参数拼成自然语言上下文，供 AI prompt 使用。
     */
    private String buildParamsContext(ConversationState state) {
        StringBuilder sb = new StringBuilder();
        state.getParamValues().forEach((key, value) ->
            sb.append(messageTagParser.getParamDisplayName(key)).append(": ").append(value).append("；")
        );
        return sb.toString();
    }

    /**
     * 从 AI 原始响应中抽取 JSON：支持 ```json 包裹 / 裸 JSON / 纯文本降级。
     */
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

    /**
     * 持久化推荐结果（新增 or 更新），并把对应 entry 写入用户 Bloom 画像。
     */
    private boolean saveRecommendationResult(String sessionId, ConversationState state, String recommendationJson) {
        try {
            RecommendationResult existing = recommendationResultMapper.findBySessionId(sessionId);
            RecommendationResult result = existing != null ? existing : new RecommendationResult();
            Map<String, String> payload = parseRecommendationPayload(recommendationJson);
            result.setSessionId(sessionId);
            result.setMode(state.getMode());
            result.setFoodName(payload.getOrDefault("foodName", "暂无推荐结果"));
            result.setReason(payload.getOrDefault("reason", "该会话暂无可展示的推荐说明"));
            if (existing == null) {
                recommendationResultMapper.insert(result);
            } else {
                recommendationResultMapper.updateById(result);
            }
            log.debug("Saved recommendation result: foodName={}", result.getFoodName());

            ConversationSession session = baseMapper.findBySessionId(sessionId);
            if (session != null) {
                Long userId = session.getUserId();
                if (userId != null) {
                    try {
                        List<CollectedParam> params = collectedParamMapper.findBySessionId(sessionId);
                        List<String> normalizedTokens = paramNormalizationService.normalizeCollectedParams(params);
                        log.debug("[{}] normalized match tokens: {}", sessionId, normalizedTokens);
                        String paramValue = String.join("|", normalizedTokens);
                        bloomFilterService.addRecommendation(userId, result.getId().toString(), paramValue);
                    } catch (Exception bloomEx) {
                        log.warn("Failed to update bloom filter for user {}: {}", userId, bloomEx.getMessage());
                    }
                }
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to save recommendation result", e);
            return false;
        }
    }

    /**
     * 解析 AI 返回的 JSON；失败回退为 foodName=原文。
     */
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

    /**
     * 构造 WebSocket 消息体（含进度条）。
     */
    private WebSocketMessage createMessage(String type, String param, String content, ConversationState state) {
        WebSocketMessage msg = new WebSocketMessage();
        msg.setType(type);
        msg.setParam(param);
        msg.setContent(content);
        msg.setProgress(createProgress(state));
        return msg;
    }

    /**
     * 构造当前进度。
     */
    private WebSocketMessage.Progress createProgress(ConversationState state) {
        return new WebSocketMessage.Progress(
                state.getCurrentQuestionCount(),
                state.getTotalQuestions(),
                state.getCollectedParams()
        );
    }

    /**
     * 写入一轮问答记录。
     */
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
            qaRecordMapper.insert(record);
        } catch (Exception e) {
            log.error("Failed to save QaRecord", e);
        }
    }

    /**
     * 写入/更新一个已收集参数（按 sessionId+paramName 唯一）。
     */
    private void saveCollectedParam(String sessionId, String paramName, String paramValue) {
        try {
            CollectedParam existing = collectedParamMapper.findBySessionIdAndParamName(sessionId, paramName);
            CollectedParam param = existing != null ? existing : new CollectedParam();
            if (existing == null) {
                param.setSessionId(sessionId);
                param.setParamName(paramName);
                param.setParamType(requiredParams.contains(paramName) ? "required" : "optional");
            }
            param.setParamValue(paramValue);
            if (existing == null) {
                collectedParamMapper.insert(param);
            } else {
                collectedParamMapper.updateById(param);
            }
        } catch (Exception e) {
            log.error("Failed to save CollectedParam", e);
        }
    }

    /**
     * 把 state 里的题号回写 session 表。
     */
    private void updateSessionQuestionCount(ConversationState state) {
        try {
            ConversationSession session = baseMapper.findBySessionId(state.getSessionId());
            if (session != null) {
                session.setCurrentQuestionCount(state.getCurrentQuestionCount());
                baseMapper.updateById(session);
            }
        } catch (Exception e) {
            log.error("Failed to update session question count", e);
        }
    }

    public List<String> getRequiredParams() { return requiredParams; }
    public List<String> getOptionalParams() { return optionalParams; }

    /**
     * 从 session 表反查 userId（取不到则抛错）。
     */
    private Long getUserIdFromSession(String sessionId) {
        ConversationSession session = baseMapper.findBySessionId(sessionId);
        if (session == null) {
            throw new RuntimeException("Session not found: " + sessionId);
        }
        return session.getUserId();
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

    /**
     * 取消会话：清理 Bloom 画像 + 软删除所有关联表数据。
     */
    @Transactional
    public void cancelSession(String sessionId) {
        log.info("[{}] canceling session - soft deleting all related data", sessionId);
        try {
            ConversationSession session = baseMapper.findBySessionId(sessionId);
            if (session != null && session.getUserId() != null) {
                Long userId = session.getUserId();
                RecommendationResult result = recommendationResultMapper.findBySessionId(sessionId);
                if (result != null && result.getId() != null) {
                    bloomFilterService.removeRecommendation(userId, result.getId().toString(), null);
                }
            }
            qaRecordMapper.softDeleteBySessionId(sessionId);
            collectedParamMapper.softDeleteBySessionId(sessionId);
            recommendationResultMapper.softDeleteBySessionId(sessionId);
            baseMapper.softDeleteBySessionId(sessionId);
            log.info("[{}] session soft deleted", sessionId);
        } catch (Exception e) {
            log.error("[{}] failed to cancel session", sessionId, e);
        }
    }
}
