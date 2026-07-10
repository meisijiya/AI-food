package com.ai.food.service.conversation;

import com.ai.food.common.mapper.CollectedParamMapper;
import com.ai.food.common.mapper.ConversationSessionMapper;
import com.ai.food.common.mapper.RecommendationResultMapper;
import com.ai.food.common.model.CollectedParam;
import com.ai.food.common.model.ConversationSession;
import com.ai.food.common.model.RecommendationResult;
import com.ai.food.dto.ConversationState;
import com.ai.food.service.ai.AiService;
import com.ai.food.service.bloom.BloomFilterService;
import com.ai.food.service.match.ParamNormalizationService;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static com.ai.food.service.conversation.ConversationUtil.OBJECT_MAPPER;
import static com.ai.food.service.conversation.ConversationUtil.REQUIRED_PARAMS;

/**
 * 对话 AI 服务：单轮 AI 确认/自由对话生成 + AI 推荐生成 + 解析 + 持久化。
 * <p>
 * 提取自原 {@code ConversationService}（618 行），按 Oracle 修订建议：facade 保留 {@code processAnswer}
 * 状态机本身，AI 调用相关的纯函数（{@code generateAiResponse} / {@code extractJsonFromResponse} /
 * {@code saveRecommendationResult}）下沉到本 service。
 * </p>
 *
 * <p>ponytail: 不引入新抽象；JSON 解析逻辑与原行为完全一致（支持 ``` 包裹 / 裸 JSON / 纯文本降级）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationAiService {

    private final AiService aiService;
    private final MessageTagParser messageTagParser;
    private final ConversationParamService paramService;
    private final RecommendationResultMapper recommendationResultMapper;
    private final ConversationSessionMapper conversationSessionMapper;
    private final CollectedParamMapper collectedParamMapper;
    private final ParamNormalizationService paramNormalizationService;
    private final BloomFilterService bloomFilterService;

    /**
     * 根据当前收集进度切换 prompt：必选参数未收齐走"简短确认"，收齐后走"自由对话"prompt。
     */
    public String generateAiResponse(String param, String answer, ConversationState state) {
        String displayName = messageTagParser.getParamDisplayName(param);
        String context = paramService.buildParamsContext(state);

        // 如果所有必选参数已收集完，让 AI 自由对话
        boolean allRequiredDone = REQUIRED_PARAMS.stream().allMatch(p ->
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
            String response = aiService.chat(systemPrompt, userPrompt).getText();
            if (response != null && !response.isBlank() && !response.startsWith("抱歉")) {
                return response.trim();
            }
        } catch (Exception e) {
            log.warn("AI response generation failed: {}", e.getMessage());
        }
        // fallback
        return String.format("好的，%s我记下了！", answer);
    }

    /**
     * 从 AI 原始响应中抽取 JSON：支持 ```json 包裹 / 裸 JSON / 纯文本降级。
     */
    public String extractJsonFromResponse(String response) {
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
    public boolean saveRecommendationResult(String sessionId, ConversationState state, String recommendationJson) {
        try {
            RecommendationResult existing = recommendationResultMapper.findBySessionId(sessionId);
            RecommendationResult result = existing != null ? existing : new RecommendationResult();
            Map<String, Object> payload = parseRecommendationPayload(recommendationJson);
            result.setSessionId(sessionId);
            // mode 已从实体删除 (A.2)
            result.setFoodName((String) payload.getOrDefault("foodName", "暂无推荐结果"));
            result.setReason((String) payload.getOrDefault("reason", "该会话暂无可展示的推荐说明"));
            // 持久化新字段: category + flavorTags (JSON 数组)
            Object flavorTagsObj = payload.get("flavorTags");
            if (flavorTagsObj instanceof List<?> flavorList) {
                result.setFlavorTags(OBJECT_MAPPER.writeValueAsString(flavorList));
            }
            result.setCategory((String) payload.get("category"));
            if (existing == null) {
                recommendationResultMapper.insert(result);
            } else {
                recommendationResultMapper.updateById(result);
            }
            log.debug("Saved recommendation result: foodName={}", result.getFoodName());

            ConversationSession session = conversationSessionMapper.findBySessionId(sessionId);
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
    private Map<String, Object> parseRecommendationPayload(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse recommendation json, using fallback text: {}", json);
            return Map.of("foodName", json, "reason", "根据您的需求为您推荐");
        }
    }
}
