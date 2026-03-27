package com.ai.food.service.conversation;

import com.ai.food.dto.ConversationState;
import com.ai.food.dto.WebSocketMessage;
import com.ai.food.repository.CollectedParamRepository;
import com.ai.food.repository.ConversationSessionRepository;
import com.ai.food.repository.QaRecordRepository;
import com.ai.food.repository.RecommendationResultRepository;
import com.ai.food.service.ai.AiService;
import com.ai.food.service.bloom.BloomFilterService;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ConversationService 对话服务")
class ConversationServiceTest {

    @Mock
    private AiService aiService;

    @Mock
    private ConversationSessionRepository conversationSessionRepository;

    @Mock
    private QaRecordRepository qaRecordRepository;

    @Mock
    private CollectedParamRepository collectedParamRepository;

    @Mock
    private RecommendationResultRepository recommendationResultRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private BloomFilterService bloomFilterService;

    private ConversationService conversationService;

    @BeforeEach
    void setUp() {
        MessageValidator messageValidator = new MessageValidator();
        MessageTagParser messageTagParser = new MessageTagParser(messageValidator, aiService);
        conversationService = new ConversationService(
                aiService, messageValidator, messageTagParser,
                conversationSessionRepository, qaRecordRepository, collectedParamRepository,
                recommendationResultRepository, redisTemplate, bloomFilterService
        );
        ReflectionTestUtils.setField(conversationService, "minQuestions", 7);
        ReflectionTestUtils.setField(conversationService, "maxQuestions", 10);
        ReflectionTestUtils.setField(conversationService, "maxParamRetry", 2);

        // 默认 mock
        when(conversationSessionRepository.findBySessionId(anyString()))
                .thenReturn(java.util.Optional.empty());
        when(qaRecordRepository.save(any())).thenReturn(null);
        when(collectedParamRepository.findBySessionIdAndParamName(anyString(), anyString()))
                .thenReturn(java.util.Optional.empty());
        when(collectedParamRepository.save(any())).thenReturn(null);
        // AI mock
        when(aiService.chat(anyString(), anyString())).thenReturn("好的，记下了！");
    }

    @Nested
    @DisplayName("初始化对话测试")
    class InitializeConversationTests {

        @Test
        @DisplayName("初始化生成合理范围的提问总数（至少7个）")
        void initializeConversation_generatesValidTotalQuestions() {
            ConversationState state = conversationService.initializeConversation("session-1");

            assertNotNull(state);
            assertEquals("session-1", state.getSessionId());
            assertTrue(state.getTotalQuestions() >= 7 && state.getTotalQuestions() <= 10);
            assertEquals("inertia", state.getMode());
        }

        @Test
        @DisplayName("首次提问正确设置参数")
        void getFirstQuestion_setsCorrectParam() {
            ConversationState state = new ConversationState("session-1", 7, "inertia");
            state.setCurrentParam("time");

            WebSocketMessage message = conversationService.getFirstQuestion(state);

            assertEquals("question", message.getType());
            assertEquals("time", message.getParam());
            assertNotNull(message.getContent());
            assertEquals(1, state.getCurrentQuestionCount());
        }
    }

    @Nested
    @DisplayName("processAnswer 有效回答测试")
    class ValidAnswerTests {

        @Test
        @DisplayName("有效回答返回两条消息：AI确认 + 下一个问题")
        void processAnswer_validAnswer_returnsConfirmAndNextQuestion() {
            ConversationState state = new ConversationState("session-1", 7, "inertia");
            state.setCurrentParam("time");
            state.incrementQuestionCount();

            List<WebSocketMessage> results = conversationService.processAnswer("session-1", "晚上", state);

            assertEquals(2, results.size());
            assertEquals("chat", results.get(0).getType());
            assertEquals("question", results.get(1).getType());
            assertEquals("location", results.get(1).getParam());
        }

        @Test
        @DisplayName("有效回答保存参数到state")
        void processAnswer_validAnswer_savesParam() {
            ConversationState state = new ConversationState("session-1", 7, "inertia");
            state.setCurrentParam("time");
            state.incrementQuestionCount();

            conversationService.processAnswer("session-1", "晚上", state);

            assertEquals("晚上", state.getParamValue("time"));
            assertTrue(state.isParamCollected("time"));
        }

        @Test
        @DisplayName("连续回答正确收集多个参数")
        void processAnswer_multipleAnswers_collectsAllParams() {
            ConversationState state = new ConversationState("session-1", 7, "inertia");
            state.setCurrentParam("time");
            state.incrementQuestionCount();

            conversationService.processAnswer("session-1", "晚上", state);
            conversationService.processAnswer("session-1", "在公司", state);
            conversationService.processAnswer("session-1", "今天晴天", state);

            assertTrue(state.isParamCollected("time"));
            assertTrue(state.isParamCollected("location"));
            assertTrue(state.isParamCollected("weather"));
        }

        @Test
        @DisplayName("AI确认消息调用AiService")
        void processAnswer_callsAiForConfirm() {
            ConversationState state = new ConversationState("session-1", 7, "inertia");
            state.setCurrentParam("time");
            state.incrementQuestionCount();

            conversationService.processAnswer("session-1", "晚上", state);

            verify(aiService, atLeastOnce()).chat(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("processAnswer 无效回答测试")
    class InvalidAnswerTests {

        @Test
        @DisplayName("null回答返回追问消息")
        void processAnswer_nullAnswer_returnsRetry() {
            ConversationState state = new ConversationState("session-1", 7, "inertia");
            state.setCurrentParam("time");
            state.incrementQuestionCount();

            List<WebSocketMessage> results = conversationService.processAnswer("session-1", null, state);

            assertFalse(results.isEmpty());
            assertEquals("2question", results.get(0).getType());
        }

        @Test
        @DisplayName("空回答返回追问消息")
        void processAnswer_emptyAnswer_returnsRetry() {
            ConversationState state = new ConversationState("session-1", 7, "inertia");
            state.setCurrentParam("time");
            state.incrementQuestionCount();

            List<WebSocketMessage> results = conversationService.processAnswer("session-1", "", state);

            assertEquals("2question", results.get(0).getType());
        }

        @Test
        @DisplayName("过短回答返回追问消息并增加重试计数")
        void processAnswer_shortAnswer_returnsRetryAndIncrementsCount() {
            ConversationState state = new ConversationState("session-1", 7, "inertia");
            state.setCurrentParam("time");
            state.incrementQuestionCount();

            List<WebSocketMessage> results = conversationService.processAnswer("session-1", "啊", state);

            assertEquals("2question", results.get(0).getType());
            assertEquals(1, state.getParamRetryCount("time"));
        }

        @Test
        @DisplayName("超过最大重试次数后跳过参数")
        void processAnswer_maxRetry_skipsParam() {
            ConversationState state = new ConversationState("session-1", 7, "inertia");
            state.setCurrentParam("weather");
            state.incrementQuestionCount();
            state.incrementParamRetry("weather");
            state.incrementParamRetry("weather");

            List<WebSocketMessage> results = conversationService.processAnswer("session-1", "???", state);

            assertEquals("未提供", state.getParamValue("weather"));
            assertFalse(results.isEmpty());
        }
    }

    @Nested
    @DisplayName("handleInterrupt 抢话测试")
    class InterruptTests {

        @Test
        @DisplayName("抢话消息返回interrupt类型")
        void handleInterrupt_returnsInterruptMessage() {
            ConversationState state = new ConversationState("session-1", 7, "inertia");
            state.setCurrentParam("time");
            state.incrementQuestionCount();

            WebSocketMessage result = conversationService.handleInterrupt("我在公司；现在是晚上", state);

            assertEquals("interrupt", result.getType());
            assertNotNull(result.getContent());
        }

        @Test
        @DisplayName("抢话消息不计入提问总数")
        void handleInterrupt_doesNotIncrementQuestionCount() {
            ConversationState state = new ConversationState("session-1", 7, "inertia");
            state.setCurrentParam("time");
            state.incrementQuestionCount();
            int countBefore = state.getCurrentQuestionCount();

            conversationService.handleInterrupt("抢话内容", state);

            assertEquals(countBefore, state.getCurrentQuestionCount());
        }
    }

    @Nested
    @DisplayName("推荐消息测试")
    class RecommendationTests {

        @Test
        @DisplayName("生成推荐消息调用AI服务")
        void generateRecommendation_callsAi() {
            ConversationState state = new ConversationState("session-1", 7, "inertia");
            state.saveParamValue("time", "晚上");
            state.incrementQuestionCount();
            when(aiService.generateRecommendation(anyString()))
                    .thenReturn("{\"foodName\":\"火锅\",\"reason\":\"适合晚上\"}");

            WebSocketMessage result = conversationService.generateRecommendationMessage("session-1", state);

            assertEquals("recommend", result.getType());
            assertTrue(result.getContent().contains("火锅"));
        }

        @Test
        @DisplayName("所有必选参数收集完毕时返回推荐")
        void processAnswer_allRequiredCollected_returnsRecommend() {
            ConversationState state = new ConversationState("session-1", 7, "inertia");
            state.setCurrentParam("taste");
            state.incrementQuestionCount();
            for (String p : new String[]{"time", "location", "weather", "mood", "companion", "budget"}) {
                state.saveParamValue(p, "test");
            }
            when(aiService.generateRecommendation(anyString()))
                    .thenReturn("{\"foodName\":\"测试\",\"reason\":\"测试\"}");

            List<WebSocketMessage> results = conversationService.processAnswer("session-1", "想吃辣的", state);

            // 最后一条应该是 recommend
            WebSocketMessage last = results.get(results.size() - 1);
            assertEquals("recommend", last.getType());
        }
    }

    @Nested
    @DisplayName("必选参数判断测试")
    class RequiredParamsTests {

        @Test
        @DisplayName("必选参数列表包含7个参数")
        void requiredParams_has7Params() {
            assertEquals(7, conversationService.getRequiredParams().size());
        }

        @Test
        @DisplayName("可选参数列表包含3个参数")
        void optionalParams_has3Params() {
            assertEquals(3, conversationService.getOptionalParams().size());
        }

        @Test
        @DisplayName("未收集完必选参数返回false")
        void isAllRequiredParamsCollected_false() {
            ConversationState state = new ConversationState("session-1", 7, "inertia");
            state.saveParamValue("time", "晚上");
            assertFalse(conversationService.isAllRequiredParamsCollected(state));
        }

        @Test
        @DisplayName("收集完所有必选参数返回true")
        void isAllRequiredParamsCollected_true() {
            ConversationState state = new ConversationState("session-1", 7, "inertia");
            for (String p : conversationService.getRequiredParams()) {
                state.saveParamValue(p, "test");
            }
            assertTrue(conversationService.isAllRequiredParamsCollected(state));
        }
    }
}
