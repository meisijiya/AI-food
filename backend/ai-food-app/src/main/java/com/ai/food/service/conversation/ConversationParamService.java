package com.ai.food.service.conversation;

import com.ai.food.common.mapper.ConversationSessionMapper;
import com.ai.food.common.model.ConversationSession;
import com.ai.food.dto.ConversationState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.ai.food.service.conversation.ConversationUtil.OPTIONAL_PARAMS;
import static com.ai.food.service.conversation.ConversationUtil.REQUIRED_PARAMS;

/**
 * 对话参数服务：7 参数收集流程 + 必选判断 + 会话权限校验。
 * <p>
 * 提取自原 {@code ConversationService}（618 行），按 Oracle 修订建议：facade 保留 70 行
 * {@code processAnswer} 状态机本身，参数相关的纯函数（{@code allParamsCollected} / {@code determineNextParam}
 * / {@code buildParamsContext}）下沉到本 service。
 * </p>
 *
 * <p>ponytail: 不引入新抽象；{@code REQUIRED_PARAMS} / {@code OPTIONAL_PARAMS} 复用 {@link ConversationUtil} 常量。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationParamService {

    private final ConversationSessionMapper conversationSessionMapper;
    private final MessageTagParser messageTagParser;

    /**
     * 校验 session 是否属于当前用户，并拒绝读取超过 30 天的已完成会话。
     */
    public void validateOwnership(String sessionId, Long userId) {
        ConversationSession session = conversationSessionMapper.findBySessionId(sessionId);
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

    /**
     * 是否所有必选 + 可选参数都已收集。
     */
    public boolean allParamsCollected(ConversationState state) {
        return REQUIRED_PARAMS.stream().allMatch(state::isParamCollected)
                && OPTIONAL_PARAMS.stream().allMatch(state::isParamCollected);
    }

    /**
     * 决定下一个待收集的参数（必选优先，自由发挥阶段后补可选参数）。
     */
    public String determineNextParam(ConversationState state) {
        for (String param : REQUIRED_PARAMS) {
            if (!state.isParamCollected(param)) return param;
        }
        if (state.isInFreeFormStage()) {
            for (String param : OPTIONAL_PARAMS) {
                if (!state.isParamCollected(param)) return param;
            }
        }
        return null;
    }

    /**
     * 把已收集参数拼成自然语言上下文，供 AI prompt 使用。
     */
    public String buildParamsContext(ConversationState state) {
        StringBuilder sb = new StringBuilder();
        state.getParamValues().forEach((key, value) ->
            sb.append(messageTagParser.getParamDisplayName(key)).append(": ").append(value).append("；")
        );
        return sb.toString();
    }

    public boolean isAllRequiredParamsCollected(ConversationState state) {
        return REQUIRED_PARAMS.stream().allMatch(state::isParamCollected);
    }

    public int getRemainingRequiredParams(ConversationState state) {
        return (int) REQUIRED_PARAMS.stream().filter(p -> !state.isParamCollected(p)).count();
    }

    public List<String> getRequiredParams() {
        return REQUIRED_PARAMS;
    }

    public List<String> getOptionalParams() {
        return OPTIONAL_PARAMS;
    }
}
