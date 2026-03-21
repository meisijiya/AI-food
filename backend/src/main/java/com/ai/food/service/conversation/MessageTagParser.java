package com.ai.food.service.conversation;

import com.ai.food.dto.ConversationState;
import com.ai.food.service.ai.AiService;
import com.ai.food.validator.MessageValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageTagParser {

    private final MessageValidator messageValidator;
    private final AiService aiService;

    private static final Map<String, String> PARAM_DISPLAY_NAMES = Map.of(
            "time", "时间",
            "location", "地点",
            "weather", "天气",
            "mood", "心情",
            "companion", "同行人",
            "budget", "预算",
            "taste", "口味",
            "restriction", "饮食禁忌",
            "preference", "偏好",
            "health", "健康需求"
    );

    private static final Map<String, String> QUESTION_TEXTS = Map.of(
            "time", "请问您现在是什么时间想吃东西呢？",
            "location", "您现在在哪里呢？",
            "weather", "现在外面天气怎么样？",
            "mood", "您现在心情如何？",
            "companion", "您是和谁一起吃饭呢？",
            "budget", "您今天的预算大概是多少？",
            "taste", "您有什么口味偏好吗？比如辣的、清淡的？"
    );

    private static final Map<String, String> RETRY_TEXTS = Map.of(
            "time", "抱歉，我不太确定您说的时间，能再说清楚一点吗？比如上午、中午或者晚上？",
            "location", "您的位置我不太明白，能告诉我更具体一点吗？",
            "weather", "天气的情况我需要更详细一点，现在是晴天还是下雨呢？",
            "mood", "您的心情能再描述一下吗？开心、难过还是平静？",
            "companion", "我不太确定您是和谁一起，是一个人还是有朋友？",
            "budget", "您的预算我不太清楚，能大概说一个范围吗？",
            "taste", "口味方面我需要了解更清楚，您喜欢辣的还是清淡的？"
    );

    public TagDecision decideTag(ConversationState state, String param, MessageValidator.ValidationResult validation) {
        log.debug(">>> decideTag() called - param: {}, validation.valid: {}, state.completed: {}",
                param, validation.isValid(), state.isCompleted());

        if (state.isCompleted()) {
            log.debug("<<< decideTag() result: RECOMMEND - all questions completed");
            return TagDecision.recommend();
        }

        if (!validation.isValid()) {
            boolean canRetry = state.canRetryParam(param, 2);
            log.debug("Validation failed - canRetry: {}, retryCount: {}",
                    canRetry, state.getParamRetryCount(param));
            if (canRetry) {
                log.debug("<<< decideTag() result: RETRY (2question)");
                return TagDecision.retry(validation.getMessage());
            } else {
                log.debug("<<< decideTag() result: SKIP_AND_CONTINUE - max retry reached");
                return TagDecision.skipAndContinue();
            }
        }

        // 自由发挥阶段：检测用户回答中是否主动提到了其他参数
        if (state.isInFreeFormStage()) {
            log.debug("In free form stage - detecting param from user answer");
            String detectedParam = messageValidator.detectParamFromAnswer(
                    state.getParamValue(param) != null ? state.getParamValue(param) : "");
            if (detectedParam != null && !detectedParam.equals(param) && !state.isParamCollected(detectedParam)) {
                log.debug("Detected uncollected param '{}' from answer, returning CHAT", detectedParam);
                return TagDecision.chat("好的，我记下了！");
            }
        }

        // 参数已收集（重复回答）时返回 chat 确认
        if (state.isParamCollected(param)) {
            log.debug("<<< decideTag() result: CHAT - param already collected");
            return TagDecision.chat("好的，收到啦！");
        }

        log.debug("<<< decideTag() result: QUESTION - normal flow");
        return TagDecision.question();
    }

    public String getQuestionContent(String param, ConversationState state) {
        log.debug("getQuestionContent() - param: {}, freeFormStage: {}", param, state.isInFreeFormStage());

        // 自由发挥阶段的可选参数：使用AI生成个性化问题
        if (state.isInFreeFormStage() && !isRequiredParam(param)) {
            log.debug("Free form stage optional param '{}', generating AI question", param);
            try {
                String context = buildContextSummary(state);
                String aiQuestion = aiService.generateQuestion(param, context);
                if (aiQuestion != null && !aiQuestion.isBlank() && !aiQuestion.startsWith("抱歉")) {
                    log.debug("AI generated question for '{}': {}", param, aiQuestion);
                    return aiQuestion.trim();
                }
            } catch (Exception e) {
                log.warn("AI question generation failed for param '{}', falling back to default", param);
            }
            // AI 回退到默认问题
            return getFreeFormQuestion(state, param);
        }

        // 必选参数：使用预定义问题
        String question = QUESTION_TEXTS.getOrDefault(param, "还有什么想告诉我的吗？");
        log.debug("Generated question for param '{}': {}", param, question);
        return question;
    }

    public String getRetryContent(String param, String originalMessage) {
        log.debug("getRetryContent() - param: {}, originalMessage: {}", param, originalMessage);
        String retryContent = RETRY_TEXTS.getOrDefault(param, "请再说清楚一点，我不太明白您的意思");
        log.debug("Generated retry content: {}", retryContent);
        return retryContent;
    }

    public String getConfirmContent(String param, String value) {
        log.debug("getConfirmContent() - param: {}, value: {}", param, value);
        String paramName = getParamDisplayName(param);
        String content = String.format("好的，%s是%s，我记下了！", paramName, value);
        log.debug("Generated confirm content: {}", content);
        return content;
    }

    private String getFreeFormQuestion(ConversationState state, String param) {
        int collected = state.getRequiredParamsCount();
        log.debug("getFreeFormQuestion() - collected required params: {}, target param: {}", collected, param);

        if (collected >= 7) {
            return switch (param) {
                case "restriction" -> "有没有什么饮食禁忌或者过敏的东西呢？比如不吃辣、花生过敏之类的？";
                case "preference" -> "最近有什么特别想吃的或者想尝试的美食吗？";
                case "health" -> "有没有健康方面的需求？比如减肥、增肌或者补充营养？";
                default -> "还有什么特别想吃的或者需要告诉我的吗？";
            };
        }
        return "还有什么想补充的吗？";
    }

    private String buildContextSummary(ConversationState state) {
        StringBuilder sb = new StringBuilder();
        state.getParamValues().forEach((key, value) -> {
            String displayName = getParamDisplayName(key);
            sb.append(displayName).append(": ").append(value).append("；");
        });
        return sb.toString();
    }

    public String getParamDisplayName(String param) {
        return PARAM_DISPLAY_NAMES.getOrDefault(param, param);
    }

    private boolean isRequiredParam(String param) {
        return QUESTION_TEXTS.containsKey(param);
    }

    public static class TagDecision {
        private final String tag;
        private final String followUpMessage;
        private final boolean shouldAdvance;

        private TagDecision(String tag, String followUpMessage, boolean shouldAdvance) {
            this.tag = tag;
            this.followUpMessage = followUpMessage;
            this.shouldAdvance = shouldAdvance;
        }

        public static TagDecision question() {
            return new TagDecision("question", null, true);
        }

        public static TagDecision retry(String message) {
            return new TagDecision("2question", message, false);
        }

        public static TagDecision chat(String message) {
            return new TagDecision("chat", message, true);
        }

        public static TagDecision recommend() {
            return new TagDecision("recommend", null, false);
        }

        public static TagDecision skipAndContinue() {
            return new TagDecision("chat", "没关系，我们继续下一个问题", true);
        }

        public static TagDecision interrupt(String message) {
            return new TagDecision("interrupt", message, false);
        }

        public String getTag() { return tag; }
        public String getFollowUpMessage() { return followUpMessage; }
        public boolean shouldAdvance() { return shouldAdvance; }
        public boolean isQuestion() { return "question".equals(tag); }
        public boolean isRetry() { return "2question".equals(tag); }
        public boolean isChat() { return "chat".equals(tag); }
        public boolean isRecommend() { return "recommend".equals(tag); }
        public boolean isInterrupt() { return "interrupt".equals(tag); }

        @Override
        public String toString() {
            return "TagDecision{tag='" + tag + "', shouldAdvance=" + shouldAdvance +
                   ", followUpMessage='" + (followUpMessage != null ? followUpMessage.substring(0, Math.min(30, followUpMessage.length())) + "..." : "null") + "'}";
        }
    }
}
