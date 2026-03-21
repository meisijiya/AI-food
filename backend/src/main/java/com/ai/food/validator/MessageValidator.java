package com.ai.food.validator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class MessageValidator {

    private static final Map<String, List<String>> PARAM_KEYWORDS = new HashMap<>();
    private static final int MIN_ANSWER_LENGTH = 2;

    static {
        PARAM_KEYWORDS.put("time", Arrays.asList(
            "早", "中", "晚", "上午", "下午", "午", "宵夜", "早餐", "午餐", "晚餐", "夜",
            "点", "点钟", "小时", "分钟", "现在", "马上", "一会儿", "过会",
            "morning", "noon", "evening", "night", "breakfast", "lunch", "dinner",
            "早餐", "午饭", "晚饭", "今日", "今天", "明天", "周末"
        ));

        PARAM_KEYWORDS.put("location", Arrays.asList(
            "家", "公司", "学校", "单位", "楼", "室", "区", "街", "路", "道", "巷",
            "商场", "超市", "饭店", "餐厅", "酒店", "宾馆", "机场", "车站", "地铁",
            "在家", "在公司", "在办公室", "在外", "出门", "出差", "旅行",
            "东", "西", "南", "北", "附近", "旁边", "对面", "里面", "外面",
            "北京", "上海", "广州", "深圳", "杭州", "成都", "重庆", "武汉"
        ));

        PARAM_KEYWORDS.put("weather", Arrays.asList(
            "晴", "阴", "雨", "雪", "风", "雾", "霾", "热", "冷", "凉", "暖",
            "阳光", "下雨", "下雪", "刮风", "大风", "小雨", "大雨", "暴雨", "暴雪",
            "高温", "低温", "严寒", "酷暑", "温暖", "凉爽", "清爽", "潮湿", "干燥",
            "天气", "气象", "温度", "湿度"
        ));

        PARAM_KEYWORDS.put("mood", Arrays.asList(
            "开心", "高兴", "快乐", "愉快", "兴奋", "激动", "惊喜", "满足", "幸福",
            "难过", "伤心", "沮丧", "郁闷", "烦躁", "焦虑", "紧张", "害怕", "担心",
            "平静", "放松", "悠闲", "无聊", "累", "疲惫", "困", "饿", "渴",
            "心情", "感觉", "情绪", "状态", "不错", "还行", "一般", "不好"
        ));

        PARAM_KEYWORDS.put("companion", Arrays.asList(
            "一个人", "独自", "自己", "单人", "单独",
            "朋友", "同事", "同学", "伙伴", "兄弟", "姐妹", "闺蜜", "哥们",
            "家人", "父母", "爸", "妈", "爸妈", "老公", "老婆", "老公", "妻子",
            "孩子", "儿子", "女儿", "宝宝", "宝贝",
            "老板", "领导", "客户", "团队", "大家", "一群人", "聚餐", "聚会"
        ));

        PARAM_KEYWORDS.put("budget", Arrays.asList(
            "元", "块", "钱", "花", "费", "预算", "便宜", "贵", "实惠", "划算",
            "经济", "节俭", "大方", "奢侈", "豪华", "普通", "一般", "中等",
            "十", "百", "千", "万",
            "免费", "不用钱", "请客", "买单", "AA", "各付各", "AA制"
        ));

        PARAM_KEYWORDS.put("taste", Arrays.asList(
            "辣", "甜", "酸", "苦", "咸", "麻", "鲜", "香", "脆", "嫩", "软",
            "清淡", "浓", "油腻", "爽口", "开胃", "麻辣", "酸辣", "甜辣",
            "重口味", "清淡", "原味", "原汁原味", "调味", "调料",
            "吃", "想吃", "喜欢", "不喜欢", "爱吃", "不吃",
            "素", "荤", "海鲜", "肉类", "蔬菜", "水果", "主食", "小吃",
            "辣的", "不辣", "微辣", "中辣", "特辣", "超辣"
        ));

        PARAM_KEYWORDS.put("restriction", Arrays.asList(
            "过敏", "不能吃", "忌口", "禁忌", "不要", "不放",
            "花生", "牛奶", "鸡蛋", "海鲜", "虾", "蟹", "贝",
            "乳糖", "麸质", "素食", "清真", "犹太",
            "生病", "糖尿病", "高血压", "减肥", "控糖", "低盐"
        ));

        PARAM_KEYWORDS.put("preference", Arrays.asList(
            "最近", "喜欢", "爱", "想吃", "想", "偏好", "偏好",
            "常吃", "经常", "突然", "特别", "特别想",
            "推荐", "试试", "新", "旧", "传统", "经典"
        ));

        PARAM_KEYWORDS.put("health", Arrays.asList(
            "健康", "营养", "补", "养生", "保健", "滋补",
            "蛋白", "维生素", "矿物质", "纤维", "低脂", "低卡",
            "减肥", "增肌", "健身", "运动", "恢复", "补充"
        ));

        log.debug("MessageValidator initialized with {} param keyword sets", PARAM_KEYWORDS.size());
    }

    public ValidationResult validate(String param, String answer) {
        log.debug(">>> validate() called - param: {}, answer: '{}'", param, answer);

        if (answer == null || answer.trim().isEmpty()) {
            log.debug("<<< validate() result: INVALID - answer is null or empty");
            return ValidationResult.invalid("回答不能为空");
        }

        String trimmedAnswer = answer.trim();
        log.debug("Trimmed answer length: {}", trimmedAnswer.length());

        if (trimmedAnswer.length() < MIN_ANSWER_LENGTH) {
            log.debug("<<< validate() result: INVALID - answer too short (min: {})", MIN_ANSWER_LENGTH);
            return ValidationResult.invalid("回答太短，请详细说明");
        }

        if (containsOnlyPunctuation(trimmedAnswer)) {
            log.debug("<<< validate() result: INVALID - answer contains only punctuation");
            return ValidationResult.invalid("请输入有效的回答");
        }

        List<String> keywords = PARAM_KEYWORDS.get(param);
        if (keywords == null) {
            log.debug("<<< validate() result: VALID - no keywords defined for param '{}'", param);
            return ValidationResult.valid();
        }

        log.debug("Checking against {} keywords for param '{}'", keywords.size(), param);
        boolean hasKeyword = keywords.stream()
                .anyMatch(keyword -> {
                    boolean contains = trimmedAnswer.contains(keyword);
                    if (contains) {
                        log.debug("  Found keyword match: '{}'", keyword);
                    }
                    return contains;
                });

        if (!hasKeyword) {
            log.debug("No keyword match found, checking general validity");
            if (!isGeneralValidAnswer(trimmedAnswer)) {
                log.debug("<<< validate() result: INVALID - no keyword match and not general valid answer");
                return ValidationResult.invalid("请回答与" + getParamName(param) + "相关的内容");
            }
            log.debug("Answer passes general validity check");
        }

        log.debug("<<< validate() result: VALID");
        return ValidationResult.valid();
    }

    public boolean containsParamKeyword(String param, String text) {
        log.debug("containsParamKeyword() - param: {}, text: '{}'", param, text);

        if (text == null || text.trim().isEmpty()) {
            log.debug("  Result: false - text is null or empty");
            return false;
        }

        List<String> keywords = PARAM_KEYWORDS.get(param);
        if (keywords == null) {
            log.debug("  Result: false - no keywords for param '{}'", param);
            return false;
        }

        boolean result = keywords.stream().anyMatch(text::contains);
        log.debug("  Result: {}", result);
        return result;
    }

    public String detectParamFromAnswer(String answer) {
        log.debug(">>> detectParamFromAnswer() called - answer: '{}'", answer);

        if (answer == null || answer.trim().isEmpty()) {
            log.debug("<<< detectParamFromAnswer() result: null - answer is null or empty");
            return null;
        }

        Map<String, Integer> paramScores = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : PARAM_KEYWORDS.entrySet()) {
            int score = 0;
            List<String> matchedKeywords = new ArrayList<>();
            for (String keyword : entry.getValue()) {
                if (answer.contains(keyword)) {
                    score++;
                    matchedKeywords.add(keyword);
                }
            }
            if (score > 0) {
                paramScores.put(entry.getKey(), score);
                log.debug("  Param '{}' scored {} with keywords: {}", entry.getKey(), score, matchedKeywords);
            }
        }

        String detectedParam = paramScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        log.debug("<<< detectParamFromAnswer() result: '{}' (score: {})", 
                detectedParam, 
                detectedParam != null ? paramScores.get(detectedParam) : 0);
        return detectedParam;
    }

    private boolean containsOnlyPunctuation(String text) {
        return text.matches("^[\\p{P}\\s]+$");
    }

    private boolean isGeneralValidAnswer(String text) {
        if (text.length() >= 5) {
            return true;
        }

        String[] generalWords = {"要", "是", "的", "了", "在", "有", "和", "或", "但", "因为", "所以"};
        return Arrays.stream(generalWords).anyMatch(text::contains);
    }

    private String getParamName(String param) {
        return switch (param) {
            case "time" -> "时间";
            case "location" -> "地点";
            case "weather" -> "天气";
            case "mood" -> "心情";
            case "companion" -> "同行人";
            case "budget" -> "预算";
            case "taste" -> "口味";
            case "restriction" -> "饮食禁忌";
            case "preference" -> "偏好";
            case "health" -> "健康需求";
            default -> param;
        };
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
