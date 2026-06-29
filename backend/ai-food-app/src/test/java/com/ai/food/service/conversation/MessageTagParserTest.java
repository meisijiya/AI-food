package com.ai.food.service.conversation;

import com.ai.food.dto.ConversationState;
import com.ai.food.service.ai.AiService;
import com.ai.food.validator.MessageValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MessageTagParser 标签解析")
class MessageTagParserTest {

    private MessageTagParser messageTagParser;
    private MessageValidator messageValidator;

    @Mock
    private AiService aiService;

    private ConversationState state;

    @BeforeEach
    void setUp() {
        messageValidator = new MessageValidator();
        messageTagParser = new MessageTagParser(messageValidator, aiService);
        state = new ConversationState("test-session", 7, "inertia");

        // 默认 mock AI 调用返回 fallback 文本
        when(aiService.generateQuestion(anyString(), anyString()))
                .thenReturn("您有什么饮食禁忌或过敏的食物吗？");
    }

    @Nested
    @DisplayName("decideTag 标签决策测试")
    class DecideTagTests {

        @Test
        @DisplayName("会话完成时返回recommend标签")
        void decideTag_completed_returnsRecommend() {
            // 使会话达到完成状态
            for (int i = 0; i < 7; i++) {
                state.incrementQuestionCount();
            }
            assertTrue(state.isCompleted());

            var validation = MessageValidator.ValidationResult.valid();
            var decision = messageTagParser.decideTag(state, "time", validation);

            assertTrue(decision.isRecommend());
        }

        @Test
        @DisplayName("校验失败且可重试返回retry标签")
        void decideTag_invalidCanRetry_returnsRetry() {
            state.setCurrentParam("time");
            state.incrementQuestionCount();

            var validation = MessageValidator.ValidationResult.invalid("请说清楚");
            var decision = messageTagParser.decideTag(state, "time", validation);

            assertTrue(decision.isRetry());
            assertEquals("请说清楚", decision.getFollowUpMessage());
        }

        @Test
        @DisplayName("校验失败且超过重试次数返回skipAndContinue（chat标签并推进）")
        void decideTag_invalidMaxRetry_returnsSkipAndContinue() {
            state.setCurrentParam("weather");
            state.incrementQuestionCount();
            // 达到最大重试次数
            state.incrementParamRetry("weather");
            state.incrementParamRetry("weather");

            var validation = MessageValidator.ValidationResult.invalid("请说清楚");
            var decision = messageTagParser.decideTag(state, "weather", validation);

            assertTrue(decision.isChat());
            assertTrue(decision.shouldAdvance()); // skipAndContinue 会推进到下一个参数
        }

        @Test
        @DisplayName("参数已收集且校验通过返回chat标签")
        void decideTag_paramAlreadyCollected_returnsChat() {
            state.setCurrentParam("time");
            state.saveParamValue("time", "晚上");
            state.incrementQuestionCount();

            var validation = MessageValidator.ValidationResult.valid();
            var decision = messageTagParser.decideTag(state, "time", validation);

            assertTrue(decision.isChat());
        }

        @Test
        @DisplayName("正常校验通过返回question标签")
        void decideTag_valid_returnsQuestion() {
            state.setCurrentParam("time");
            state.incrementQuestionCount();

            var validation = MessageValidator.ValidationResult.valid();
            var decision = messageTagParser.decideTag(state, "time", validation);

            assertTrue(decision.isQuestion());
        }
    }

    @Nested
    @DisplayName("getQuestionContent 问题内容生成测试")
    class QuestionContentTests {

        @Test
        @DisplayName("时间参数返回正确的问题")
        void getQuestionContent_time_returnsTimeQuestion() {
            String content = messageTagParser.getQuestionContent("time", state);
            assertEquals("请问您现在是什么时间想吃东西呢？", content);
        }

        @Test
        @DisplayName("地点参数返回正确的问题")
        void getQuestionContent_location_returnsLocationQuestion() {
            String content = messageTagParser.getQuestionContent("location", state);
            assertEquals("您现在在哪里呢？", content);
        }

        @Test
        @DisplayName("天气参数返回正确的问题")
        void getQuestionContent_weather_returnsWeatherQuestion() {
            String content = messageTagParser.getQuestionContent("weather", state);
            assertEquals("现在外面天气怎么样？", content);
        }

        @Test
        @DisplayName("心情参数返回正确的问题")
        void getQuestionContent_mood_returnsMoodQuestion() {
            String content = messageTagParser.getQuestionContent("mood", state);
            assertEquals("您现在心情如何？", content);
        }

        @Test
        @DisplayName("同行人参数返回正确的问题")
        void getQuestionContent_companion_returnsCompanionQuestion() {
            String content = messageTagParser.getQuestionContent("companion", state);
            assertEquals("您是和谁一起吃饭呢？", content);
        }

        @Test
        @DisplayName("预算参数返回正确的问题")
        void getQuestionContent_budget_returnsBudgetQuestion() {
            String content = messageTagParser.getQuestionContent("budget", state);
            assertEquals("您今天的预算大概是多少？", content);
        }

        @Test
        @DisplayName("口味参数返回正确的问题")
        void getQuestionContent_taste_returnsTasteQuestion() {
            String content = messageTagParser.getQuestionContent("taste", state);
            assertEquals("您有什么口味偏好吗？比如辣的、清淡的？", content);
        }

        @Test
        @DisplayName("自由发挥阶段可选参数使用AI生成问题")
        void getQuestionContent_freeFormStage_usesAiForOptionalParam() {
            state.enterFreeFormStage();
            // 收集了7个必选参数
            for (String p : new String[]{"time", "location", "weather", "mood", "companion", "budget", "taste"}) {
                state.saveParamValue(p, "test");
            }

            when(aiService.generateQuestion(anyString(), anyString()))
                    .thenReturn("您有什么饮食禁忌或过敏的食物吗？");

            String content = messageTagParser.getQuestionContent("restriction", state);
            assertEquals("您有什么饮食禁忌或过敏的食物吗？", content);
        }

        @Test
        @DisplayName("自由发挥阶段AI失败时回退到默认问题")
        void getQuestionContent_freeFormStage_aiFails_fallback() {
            state.enterFreeFormStage();
            for (String p : new String[]{"time", "location", "weather", "mood", "companion", "budget", "taste"}) {
                state.saveParamValue(p, "test");
            }

            when(aiService.generateQuestion(anyString(), anyString()))
                    .thenReturn("抱歉，AI服务暂时不可用");

            String content = messageTagParser.getQuestionContent("restriction", state);
            // AI 返回了"抱歉"开头的内容，应使用默认 fallback
            assertTrue(content.contains("饮食禁忌") || content.contains("还有什么"));
        }

        @Test
        @DisplayName("自由发挥阶段未收集完必选参数回退到默认补充问题")
        void getQuestionContent_freeFormPartial_fallback() {
            state.enterFreeFormStage();
            state.saveParamValue("time", "晚上");

            when(aiService.generateQuestion(anyString(), anyString()))
                    .thenReturn("还有什么想补充的吗？");

            String content = messageTagParser.getQuestionContent("preference", state);
            assertNotNull(content);
            assertFalse(content.isEmpty());
        }

        @Test
        @DisplayName("未知参数返回默认问题")
        void getQuestionContent_unknown_returnsDefault() {
            String content = messageTagParser.getQuestionContent("unknown", state);
            assertEquals("还有什么想告诉我的吗？", content);
        }
    }

    @Nested
    @DisplayName("getRetryContent 追问内容生成测试")
    class RetryContentTests {

        @Test
        @DisplayName("时间追问返回正确内容")
        void getRetryContent_time_returnsTimeRetry() {
            String content = messageTagParser.getRetryContent("time", "没听懂");
            assertTrue(content.contains("时间"));
            assertTrue(content.contains("上午") || content.contains("中午") || content.contains("晚上"));
        }

        @Test
        @DisplayName("地点追问返回正确内容")
        void getRetryContent_location_returnsLocationRetry() {
            String content = messageTagParser.getRetryContent("location", "没听懂");
            assertTrue(content.contains("位置"));
        }

        @Test
        @DisplayName("天气追问返回正确内容")
        void getRetryContent_weather_returnsWeatherRetry() {
            String content = messageTagParser.getRetryContent("weather", "没听懂");
            assertTrue(content.contains("天气"));
        }

        @Test
        @DisplayName("心情追问返回正确内容")
        void getRetryContent_mood_returnsMoodRetry() {
            String content = messageTagParser.getRetryContent("mood", "没听懂");
            assertTrue(content.contains("心情"));
        }

        @Test
        @DisplayName("预算追问返回正确内容")
        void getRetryContent_budget_returnsBudgetRetry() {
            String content = messageTagParser.getRetryContent("budget", "没听懂");
            assertTrue(content.contains("预算"));
        }

        @Test
        @DisplayName("口味追问返回正确内容")
        void getRetryContent_taste_returnsTasteRetry() {
            String content = messageTagParser.getRetryContent("taste", "没听懂");
            assertTrue(content.contains("口味"));
        }

        @Test
        @DisplayName("未知参数追问返回通用内容")
        void getRetryContent_unknown_returnsDefault() {
            String content = messageTagParser.getRetryContent("unknown", "没听懂");
            assertNotNull(content);
            assertFalse(content.isEmpty());
        }
    }

    @Nested
    @DisplayName("getConfirmContent 确认内容生成测试")
    class ConfirmContentTests {

        @Test
        @DisplayName("确认时间参数")
        void getConfirmContent_time_returnsConfirm() {
            String content = messageTagParser.getConfirmContent("time", "晚上");
            assertTrue(content.contains("时间"));
            assertTrue(content.contains("晚上"));
            assertTrue(content.contains("记下了"));
        }

        @Test
        @DisplayName("确认地点参数")
        void getConfirmContent_location_returnsConfirm() {
            String content = messageTagParser.getConfirmContent("location", "公司");
            assertTrue(content.contains("地点"));
            assertTrue(content.contains("公司"));
        }

        @Test
        @DisplayName("确认口味参数")
        void getConfirmContent_taste_returnsConfirm() {
            String content = messageTagParser.getConfirmContent("taste", "辣的");
            assertTrue(content.contains("口味"));
            assertTrue(content.contains("辣的"));
        }

        @Test
        @DisplayName("确认预算参数")
        void getConfirmContent_budget_returnsConfirm() {
            String content = messageTagParser.getConfirmContent("budget", "50元以内");
            assertTrue(content.contains("预算"));
            assertTrue(content.contains("50元以内"));
        }
    }

    @Nested
    @DisplayName("TagDecision 标签决策对象测试")
    class TagDecisionTests {

        @Test
        @DisplayName("question标签应推进")
        void question_shouldAdvance() {
            var decision = MessageTagParser.TagDecision.question();
            assertTrue(decision.isQuestion());
            assertTrue(decision.shouldAdvance());
            assertNull(decision.getFollowUpMessage());
        }

        @Test
        @DisplayName("retry标签不应推进")
        void retry_shouldNotAdvance() {
            var decision = MessageTagParser.TagDecision.retry("重试消息");
            assertTrue(decision.isRetry());
            assertFalse(decision.shouldAdvance());
            assertEquals("重试消息", decision.getFollowUpMessage());
        }

        @Test
        @DisplayName("chat标签应推进")
        void chat_shouldAdvance() {
            var decision = MessageTagParser.TagDecision.chat("确认消息");
            assertTrue(decision.isChat());
            assertTrue(decision.shouldAdvance());
            assertEquals("确认消息", decision.getFollowUpMessage());
        }

        @Test
        @DisplayName("recommend标签不应推进")
        void recommend_shouldNotAdvance() {
            var decision = MessageTagParser.TagDecision.recommend();
            assertTrue(decision.isRecommend());
            assertFalse(decision.shouldAdvance());
        }

        @Test
        @DisplayName("skipAndContinue返回chat标签并应推进")
        void skipAndContinue_isChatAndAdvances() {
            var decision = MessageTagParser.TagDecision.skipAndContinue();
            assertTrue(decision.isChat());
            assertTrue(decision.shouldAdvance());
        }

        @Test
        @DisplayName("interrupt标签不应推进")
        void interrupt_shouldNotAdvance() {
            var decision = MessageTagParser.TagDecision.interrupt("打断消息");
            assertTrue(decision.isInterrupt());
            assertFalse(decision.shouldAdvance());
        }

        @Test
        @DisplayName("toString不抛异常")
        void toString_doesNotThrow() {
            var decision = MessageTagParser.TagDecision.chat("这是一条很长很长很长的消息用来测试截断逻辑");
            assertDoesNotThrow(decision::toString);
            assertNotNull(decision.toString());
        }
    }
}
