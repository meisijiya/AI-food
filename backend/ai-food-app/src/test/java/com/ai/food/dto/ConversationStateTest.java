package com.ai.food.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConversationState 对话状态管理")
class ConversationStateTest {

    private ConversationState state;

    @BeforeEach
    void setUp() {
        state = new ConversationState("test-session-001", 7, "inertia");
    }

    @Nested
    @DisplayName("初始化测试")
    class InitializationTests {

        @Test
        @DisplayName("构造函数正确设置初始值")
        void constructor_setsInitialValues() {
            assertEquals("test-session-001", state.getSessionId());
            assertEquals(7, state.getTotalQuestions());
            assertEquals("inertia", state.getMode());
            assertEquals(0, state.getCurrentQuestionCount());
            assertFalse(state.getAiProcessing());
            assertFalse(state.isInFreeFormStage());
            assertEquals(0, state.getInterruptCount());
            assertTrue(state.getCollectedParams().isEmpty());
            assertTrue(state.getParamValues().isEmpty());
            assertTrue(state.getPendingMessages().isEmpty());
        }

        @Test
        @DisplayName("随机模式初始化")
        void constructor_randomMode() {
            ConversationState randomState = new ConversationState("session-2", 5, "random");
            assertEquals("random", randomState.getMode());
            assertEquals(5, randomState.getTotalQuestions());
        }
    }

    @Nested
    @DisplayName("参数收集测试")
    class ParamCollectionTests {

        @Test
        @DisplayName("保存参数值并添加到已收集列表")
        void saveParamValue_savesValueAndAddsToCollected() {
            state.saveParamValue("time", "晚上");

            assertEquals("晚上", state.getParamValue("time"));
            assertTrue(state.isParamCollected("time"));
            assertEquals(1, state.getCollectedParams().size());
        }

        @Test
        @DisplayName("保存多个参数")
        void saveParamValue_multipleParams() {
            state.saveParamValue("time", "晚上");
            state.saveParamValue("location", "公司");
            state.saveParamValue("weather", "晴天");

            assertEquals(3, state.getCollectedParams().size());
            assertTrue(state.isParamCollected("time"));
            assertTrue(state.isParamCollected("location"));
            assertTrue(state.isParamCollected("weather"));
            assertFalse(state.isParamCollected("mood"));
        }

        @Test
        @DisplayName("重复保存同一参数不重复添加到列表")
        void saveParamValue_duplicateParam_doesNotAddTwice() {
            state.saveParamValue("time", "晚上");
            state.saveParamValue("time", "中午");

            assertEquals("中午", state.getParamValue("time"));
            assertEquals(1, state.getCollectedParams().size());
        }

        @Test
        @DisplayName("已收集必选参数计数正确")
        void getRequiredParamsCount_countsOnlyRequired() {
            state.saveParamValue("time", "晚上");
            state.saveParamValue("location", "公司");
            state.saveParamValue("restriction", "不吃辣");

            assertEquals(2, state.getRequiredParamsCount());
        }

        @Test
        @DisplayName("isRequiredParam 正确区分必选和可选参数")
        void isRequiredParam_distinguishesRequiredAndOptional() {
            assertTrue(state.isRequiredParam("time"));
            assertTrue(state.isRequiredParam("budget"));
            assertFalse(state.isRequiredParam("restriction"));
            assertFalse(state.isRequiredParam("health"));
        }
    }

    @Nested
    @DisplayName("提问计数测试")
    class QuestionCountTests {

        @Test
        @DisplayName("提问计数递增")
        void incrementQuestionCount_increments() {
            assertEquals(0, state.getCurrentQuestionCount());
            state.incrementQuestionCount();
            assertEquals(1, state.getCurrentQuestionCount());
            state.incrementQuestionCount();
            assertEquals(2, state.getCurrentQuestionCount());
        }

        @Test
        @DisplayName("未达到总次数时未完成")
        void isCompleted_falseWhenBelowTotal() {
            for (int i = 0; i < 6; i++) {
                state.incrementQuestionCount();
            }
            assertFalse(state.isCompleted());
        }

        @Test
        @DisplayName("达到总次数时标记完成")
        void isCompleted_trueWhenReachedTotal() {
            for (int i = 0; i < 7; i++) {
                state.incrementQuestionCount();
            }
            assertTrue(state.isCompleted());
        }

        @Test
        @DisplayName("超过总次数时也标记完成")
        void isCompleted_trueWhenExceededTotal() {
            for (int i = 0; i < 10; i++) {
                state.incrementQuestionCount();
            }
            assertTrue(state.isCompleted());
        }
    }

    @Nested
    @DisplayName("参数重试测试")
    class ParamRetryTests {

        @Test
        @DisplayName("初始重试次数为0")
        void getParamRetryCount_initialIsZero() {
            assertEquals(0, state.getParamRetryCount("time"));
        }

        @Test
        @DisplayName("重试次数递增")
        void incrementParamRetry_increments() {
            state.incrementParamRetry("time");
            assertEquals(1, state.getParamRetryCount("time"));
            state.incrementParamRetry("time");
            assertEquals(2, state.getParamRetryCount("time"));
        }

        @Test
        @DisplayName("不同参数重试次数独立计数")
        void incrementParamRetry_independentPerParam() {
            state.incrementParamRetry("time");
            state.incrementParamRetry("time");
            state.incrementParamRetry("location");

            assertEquals(2, state.getParamRetryCount("time"));
            assertEquals(1, state.getParamRetryCount("location"));
        }

        @Test
        @DisplayName("未超过最大重试次数时可重试")
        void canRetryParam_trueWhenBelowMax() {
            state.incrementParamRetry("time");
            assertTrue(state.canRetryParam("time", 2));
        }

        @Test
        @DisplayName("达到最大重试次数时不可重试")
        void canRetryParam_falseWhenReachedMax() {
            state.incrementParamRetry("time");
            state.incrementParamRetry("time");
            assertFalse(state.canRetryParam("time", 2));
        }
    }

    @Nested
    @DisplayName("打断机制测试")
    class InterruptTests {

        @Test
        @DisplayName("初始可打断")
        void canInterrupt_trueInitially() {
            assertTrue(state.canInterrupt());
        }

        @Test
        @DisplayName("打断次数递增")
        void incrementInterruptCount_increments() {
            state.incrementInterruptCount();
            assertEquals(1, state.getInterruptCount());
            assertTrue(state.canInterrupt());
        }

        @Test
        @DisplayName("达到10次打断上限后不可打断")
        void canInterrupt_falseAtLimit() {
            for (int i = 0; i < 10; i++) {
                state.incrementInterruptCount();
            }
            assertFalse(state.canInterrupt());
        }

        @Test
        @DisplayName("待处理消息队列操作")
        void pendingMessages_operations() {
            assertFalse(state.hasPendingMessages());

            state.addPendingMessage("消息1");
            assertTrue(state.hasPendingMessages());
            assertEquals(1, state.getPendingMessages().size());

            state.addPendingMessage("消息2");
            assertEquals(2, state.getPendingMessages().size());

            state.clearPendingMessages();
            assertFalse(state.hasPendingMessages());
            assertEquals(0, state.getPendingMessages().size());
        }
    }

    @Nested
    @DisplayName("自由发挥阶段测试")
    class FreeFormStageTests {

        @Test
        @DisplayName("初始不在自由发挥阶段")
        void enterFreeFormStage_initiallyFalse() {
            assertFalse(state.isInFreeFormStage());
        }

        @Test
        @DisplayName("进入自由发挥阶段")
        void enterFreeFormStage_setsTrue() {
            state.enterFreeFormStage();
            assertTrue(state.isInFreeFormStage());
        }
    }

    @Nested
    @DisplayName("当前参数管理测试")
    class CurrentParamTests {

        @Test
        @DisplayName("设置和获取当前参数")
        void setCurrentParam_getCurrentParam() {
            state.setCurrentParam("time");
            assertEquals("time", state.getCurrentParam());

            state.setCurrentParam("location");
            assertEquals("location", state.getCurrentParam());
        }
    }
}
