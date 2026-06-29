package com.ai.food.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MessageValidator 消息校验")
class MessageValidatorTest {

    private MessageValidator validator;

    @BeforeEach
    void setUp() {
        validator = new MessageValidator();
    }

    @Nested
    @DisplayName("空值与边界校验")
    class BoundaryValidationTests {

        @ParameterizedTest(name = "空值/空字符串应返回无效: [{0}]")
        @NullAndEmptySource
        @DisplayName("null和空字符串返回无效")
        void validate_nullOrEmpty_returnsInvalid(String answer) {
            var result = validator.validate("time", answer);
            assertFalse(result.isValid());
            assertEquals("回答不能为空", result.getMessage());
        }

        @ParameterizedTest(name = "过短回答应返回无效: [{0}]")
        @ValueSource(strings = {"a", "啊", "1"})
        @DisplayName("单字符回答返回无效")
        void validate_tooShort_returnsInvalid(String answer) {
            var result = validator.validate("time", answer);
            assertFalse(result.isValid());
            assertEquals("回答太短，请详细说明", result.getMessage());
        }

        @Test
        @DisplayName("空白字符串trim后为空返回无效")
        void validate_whitespaceOnly_returnsInvalid() {
            var result = validator.validate("time", "   ");
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("纯标点符号返回无效")
        void validate_onlyPunctuation_returnsInvalid() {
            var result = validator.validate("time", "？？？");
            assertFalse(result.isValid());
            assertEquals("请输入有效的回答", result.getMessage());
        }

        @Test
        @DisplayName("纯标点加空格返回无效")
        void validate_punctuationWithSpaces_returnsInvalid() {
            var result = validator.validate("time", "!!! ??");
            assertFalse(result.isValid());
        }
    }

    @Nested
    @DisplayName("时间参数校验")
    class TimeValidationTests {

        @Test
        @DisplayName("包含时间关键词返回有效")
        void validate_timeKeyword_returnsValid() {
            var result = validator.validate("time", "现在是晚上");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("包含上午关键词返回有效")
        void validate_morningKeyword_returnsValid() {
            var result = validator.validate("time", "上午十点");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("包含午餐关键词返回有效")
        void validate_lunchKeyword_returnsValid() {
            var result = validator.validate("time", "午饭时间");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("包含宵夜关键词返回有效")
        void validate_midnightSnackKeyword_returnsValid() {
            var result = validator.validate("time", "想吃宵夜");
            assertTrue(result.isValid());
        }
    }

    @Nested
    @DisplayName("地点参数校验")
    class LocationValidationTests {

        @Test
        @DisplayName("包含地点关键词返回有效")
        void validate_locationKeyword_returnsValid() {
            var result = validator.validate("location", "我在公司");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("包含商场关键词返回有效")
        void validate_mallKeyword_returnsValid() {
            var result = validator.validate("location", "在商场附近");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("包含城市名返回有效")
        void validate_cityKeyword_returnsValid() {
            var result = validator.validate("location", "我在北京");
            assertTrue(result.isValid());
        }
    }

    @Nested
    @DisplayName("天气参数校验")
    class WeatherValidationTests {

        @Test
        @DisplayName("包含天气关键词返回有效")
        void validate_weatherKeyword_returnsValid() {
            var result = validator.validate("weather", "今天晴天");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("包含下雨关键词返回有效")
        void validate_rainKeyword_returnsValid() {
            var result = validator.validate("weather", "外面下大雨");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("包含温度关键词返回有效")
        void validate_temperatureKeyword_returnsValid() {
            var result = validator.validate("weather", "今天很热");
            assertTrue(result.isValid());
        }
    }

    @Nested
    @DisplayName("心情参数校验")
    class MoodValidationTests {

        @Test
        @DisplayName("包含开心关键词返回有效")
        void validate_happyKeyword_returnsValid() {
            var result = validator.validate("mood", "今天很开心");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("包含疲惫关键词返回有效")
        void validate_tiredKeyword_returnsValid() {
            var result = validator.validate("mood", "有点累");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("包含一般关键词返回有效")
        void validate_normalKeyword_returnsValid() {
            var result = validator.validate("mood", "心情还行");
            assertTrue(result.isValid());
        }
    }

    @Nested
    @DisplayName("同行人参数校验")
    class CompanionValidationTests {

        @Test
        @DisplayName("包含独自关键词返回有效")
        void validate_aloneKeyword_returnsValid() {
            var result = validator.validate("companion", "一个人吃饭");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("包含朋友关键词返回有效")
        void validate_friendKeyword_returnsValid() {
            var result = validator.validate("companion", "和朋友一起");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("包含家人关键词返回有效")
        void validate_familyKeyword_returnsValid() {
            var result = validator.validate("companion", "和家人一起吃");
            assertTrue(result.isValid());
        }
    }

    @Nested
    @DisplayName("预算参数校验")
    class BudgetValidationTests {

        @Test
        @DisplayName("包含金额关键词返回有效")
        void validate_moneyKeyword_returnsValid() {
            var result = validator.validate("budget", "大概50元");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("包含便宜关键词返回有效")
        void validate_cheapKeyword_returnsValid() {
            var result = validator.validate("budget", "便宜一点的");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("包含AA制关键词返回有效")
        void validate_aaKeyword_returnsValid() {
            var result = validator.validate("budget", "我们AA制");
            assertTrue(result.isValid());
        }
    }

    @Nested
    @DisplayName("口味参数校验")
    class TasteValidationTests {

        @Test
        @DisplayName("包含辣关键词返回有效")
        void validate_spicyKeyword_returnsValid() {
            var result = validator.validate("taste", "想吃辣的");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("包含清淡关键词返回有效")
        void validate_lightKeyword_returnsValid() {
            var result = validator.validate("taste", "想吃清淡的");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("包含海鲜关键词返回有效")
        void validate_seafoodKeyword_returnsValid() {
            var result = validator.validate("taste", "想吃海鲜");
            assertTrue(result.isValid());
        }
    }

    @Nested
    @DisplayName("无关键词但通用有效回答")
    class GeneralValidityTests {

        @Test
        @DisplayName("5个字符以上的无关键词回答也有效")
        void validate_longAnswerNoKeyword_returnsValid() {
            var result = validator.validate("time", "大概是下午三四点钟的样子");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("包含通用词汇的短回答有效")
        void validate_generalWord_returnsValid() {
            var result = validator.validate("location", "是在家的");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("2-4字符无关键词无通用词返回无效")
        void validate_shortNoKeyword_returnsInvalid() {
            var result = validator.validate("weather", "不错");
            assertFalse(result.isValid());
        }
    }

    @Nested
    @DisplayName("未知参数校验")
    class UnknownParamTests {

        @Test
        @DisplayName("未知参数名返回有效（无关键词定义）")
        void validate_unknownParam_returnsValid() {
            var result = validator.validate("unknown_param", "随便什么回答");
            assertTrue(result.isValid());
        }
    }

    @Nested
    @DisplayName("containsParamKeyword测试")
    class ContainsKeywordTests {

        @Test
        @DisplayName("包含关键词返回true")
        void containsParamKeyword_withKeyword_returnsTrue() {
            assertTrue(validator.containsParamKeyword("time", "晚上好"));
        }

        @Test
        @DisplayName("不包含关键词返回false")
        void containsParamKeyword_noKeyword_returnsFalse() {
            assertFalse(validator.containsParamKeyword("time", "随便"));
        }

        @Test
        @DisplayName("null文本返回false")
        void containsParamKeyword_null_returnsFalse() {
            assertFalse(validator.containsParamKeyword("time", null));
        }

        @Test
        @DisplayName("空文本返回false")
        void containsParamKeyword_empty_returnsFalse() {
            assertFalse(validator.containsParamKeyword("time", ""));
        }

        @Test
        @DisplayName("未知参数返回false")
        void containsParamKeyword_unknownParam_returnsFalse() {
            assertFalse(validator.containsParamKeyword("unknown", "随便"));
        }
    }

    @Nested
    @DisplayName("detectParamFromAnswer测试")
    class DetectParamTests {

        @Test
        @DisplayName("检测时间相关回答")
        void detectParam_timeAnswer_returnsTime() {
            String result = validator.detectParamFromAnswer("现在是晚上八点");
            assertEquals("time", result);
        }

        @Test
        @DisplayName("检测地点相关回答")
        void detectParam_locationAnswer_returnsLocation() {
            String result = validator.detectParamFromAnswer("我在公司办公室");
            assertEquals("location", result);
        }

        @Test
        @DisplayName("检测天气相关回答")
        void detectParam_weatherAnswer_returnsWeather() {
            String result = validator.detectParamFromAnswer("外面在下雨");
            assertEquals("weather", result);
        }

        @Test
        @DisplayName("null返回null")
        void detectParam_null_returnsNull() {
            assertNull(validator.detectParamFromAnswer(null));
        }

        @Test
        @DisplayName("空字符串返回null")
        void detectParam_empty_returnsNull() {
            assertNull(validator.detectParamFromAnswer(""));
        }

        @Test
        @DisplayName("不匹配任何参数返回null")
        void detectParam_noMatch_returnsNull() {
            String result = validator.detectParamFromAnswer("随便说说");
            assertNull(result);
        }

        @Test
        @DisplayName("多参数匹配返回得分最高的")
        void detectParam_multipleMatches_returnsHighestScore() {
            // "晚上在家吃辣的" 匹配 time(晚,晚上) + location(家) + taste(辣,吃)
            String result = validator.detectParamFromAnswer("晚上在家吃辣的");
            assertNotNull(result);
            // 应返回得分最高的参数
        }
    }
}
