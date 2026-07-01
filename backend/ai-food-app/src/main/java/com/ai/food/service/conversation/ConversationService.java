package com.ai.food.service.conversation;

import com.ai.food.common.mapper.CollectedParamMapper;
import com.ai.food.common.mapper.ConversationSessionMapper;
import com.ai.food.common.mapper.QaRecordMapper;
import com.ai.food.common.mapper.RecommendationResultMapper;
import com.ai.food.common.model.CollectedParam;
import com.ai.food.common.model.ConversationSession;
import com.ai.food.common.model.QaRecord;
import com.ai.food.common.model.RecommendationResult;
import com.ai.food.dto.ConversationState;
import com.ai.food.dto.WebSocketMessage;
import com.ai.food.service.ai.AiService;
import com.ai.food.service.bloom.BloomFilterService;
import com.ai.food.validator.MessageValidator;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.ai.food.service.conversation.ConversationUtil.PENDING_RECOMMEND_KEY;
import static com.ai.food.service.conversation.ConversationUtil.REQUIRED_PARAMS_COUNT;

/**
 * 对话会话服务 facade：保留原 618 行 {@code ConversationService} 的全部公开方法签名（含 70 行
 * {@code processAnswer} 状态机本身，按 Oracle 修订建议不拆），内部按职责 delegate 到
 * {@link ConversationParamService} / {@link ConversationAiService}。
 * <p>
 * Controller / {@code ConversationWebSocketHandler} 等调用方无需修改，仍按原
 * {@code conversationService.xxx(...)} 调用。
 * </p>
 *
 * <p>ponytail: {@code processAnswer} 70 行状态机不拆 — Oracle 明确"应留在 facade，只把叶子调用抽出去"。
 * Redis key 常量与必选/可选参数列表已下沉到 {@link ConversationUtil}。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService extends ServiceImpl<ConversationSessionMapper, ConversationSession> {

    private final ConversationParamService paramService;
    private final ConversationAiService aiSubService;
    private final AiService aiService;
    private final MessageValidator messageValidator;
    private final MessageTagParser messageTagParser;
    private final QaRecordMapper qaRecordMapper;
    private final CollectedParamMapper collectedParamMapper;
    private final RecommendationResultMapper recommendationResultMapper;
    private final StringRedisTemplate redisTemplate;
    private final BloomFilterService bloomFilterService;

    @Value("${ai.conversation.min-questions:7}")
    private int minQuestions;

    @Value("${ai.conversation.max-questions:10}")
    private int maxQuestions;

    @Value("${ai.conversation.max-params-retry:2}")
    private int maxParamRetry;

    // ==================== 权限校验 ====================

    /**
     * 校验 session 是否属于当前用户，并拒绝读取超过 30 天的已完成会话。
     */
    public void validateOwnership(String sessionId, Long userId) {
        paramService.validateOwnership(sessionId, userId);
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
    // ponytail: 70 行状态机本身不拆 — Oracle 修订建议保留在 facade，只 delegate 叶子调用。

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
                        messageTagParser.getQuestionContent(currentParam, state), answer,
                        state.getCurrentQuestionCount(), null, null, null, null);
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
                    messageTagParser.getQuestionContent(currentParam, state), answer,
                    state.getCurrentQuestionCount(), null, null, null, null);
            saveCollectedParam(sessionId, currentParam, answer);
        }

        // 2. 检查是否进入自由发挥阶段
        if (state.getCurrentQuestionCount() >= REQUIRED_PARAMS_COUNT && !state.isInFreeFormStage()) {
            state.enterFreeFormStage();
            log.info("[{}] entered free form stage", sessionId);
        }

        // 3. 调用 AI 生成个性化确认/闲聊消息
        String aiResponse = aiSubService.generateAiResponse(currentParam, answer, state);
        result.add(createMessage("chat", currentParam, aiResponse, state));

        // 4. 确定下一个问题或进入推荐
        if (state.isCompleted() || paramService.allParamsCollected(state)) {
            result.add(generateRecommendationMessage(sessionId, state));
            return result;
        }

        String nextParam = paramService.determineNextParam(state);
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
        String context = paramService.buildParamsContext(state);
        String prompt = String.format(
            "用户刚才快速发了几条消息：「%s」。已收集信息：%s。" +
            "请用一句轻松的话回应，比如「你说话太快啦」或者直接汇总理解用户的意思。15字以内。",
            combinedMessage, context
        );

        String aiReply;
        try {
            aiReply = aiService.chat("你是一个友好的美食推荐助手。", prompt).getText();
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

        String normalizedJson = aiSubService.extractJsonFromResponse(aiResponse);
        boolean saved = aiSubService.saveRecommendationResult(sessionId, state, normalizedJson);

        if (!saved) {
            WebSocketMessage msg = new WebSocketMessage();
            msg.setType("error");
            msg.setContent("推荐结果保存失败，请稍后重试");
            msg.setProgress(createProgress(state));
            return msg;
        }

        try {
            Long userId = getUserIdFromSession(sessionId);
            String cacheKey = PENDING_RECOMMEND_KEY + userId;
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

    // ==================== 工具方法（facade 私有 helper） ====================

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
     * <p>
     * token / model 字段由 LLM 生成问答的调用方填值；intake Q&A（question/2question）的调用方传 null。
     * 移除了旧的 {@code isValid} 入参——{@code question_type} 已经能区分 valid ("question") 与
     * retry ("2question")，业务字段冗余且与新增的 token 字段叠加后超过 MP 默认 INSERT 列数也易踩坑。
     * </p>
     */
    private void saveQaRecord(String sessionId, String questionType, String paramName,
                              String aiQuestion, String userAnswer, Integer questionOrder,
                              Long promptTokens, Long completionTokens, Long totalTokens, String model) {
        try {
            QaRecord record = new QaRecord();
            record.setSessionId(sessionId);
            record.setQuestionType(questionType);
            record.setParamName(paramName);
            record.setAiQuestion(aiQuestion);
            record.setUserAnswer(userAnswer);
            record.setQuestionOrder(questionOrder);
            record.setPromptTokens(promptTokens != null ? promptTokens.intValue() : null);
            record.setCompletionTokens(completionTokens != null ? completionTokens.intValue() : null);
            record.setTotalTokens(totalTokens != null ? totalTokens.intValue() : null);
            record.setModel(model);
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
                param.setParamType(ConversationUtil.REQUIRED_PARAMS.contains(paramName) ? "required" : "optional");
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

    // ==================== 必选参数查询（delegate → paramService） ====================

    public List<String> getRequiredParams() { return paramService.getRequiredParams(); }

    public List<String> getOptionalParams() { return paramService.getOptionalParams(); }

    public boolean isAllRequiredParamsCollected(ConversationState state) {
        return paramService.isAllRequiredParamsCollected(state);
    }

    public int getRemainingRequiredParams(ConversationState state) {
        return paramService.getRemainingRequiredParams(state);
    }

    // ==================== 取消会话 ====================

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
