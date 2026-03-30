package com.ai.food.service.match;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于确定性规则将自由回答映射为稳定标签，优先覆盖高频且容易枚举的参数。
 */
@Component
public class RuleBasedParamNormalizer {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");

    /**
     * 将单个参数的自由文本按规则标准化为 token 列表。
     */
    public List<String> normalize(String paramName, String rawValue) {
        if (paramName == null || rawValue == null) {
            return List.of();
        }

        String cleanedParam = paramName.trim().toLowerCase(Locale.ROOT);
        String cleanedValue = rawValue.trim().toLowerCase(Locale.ROOT);
        if (cleanedParam.isEmpty() || cleanedValue.isEmpty()) {
            return List.of();
        }

        return switch (cleanedParam) {
            case "time" -> singleToken(cleanedParam, normalizeTime(cleanedValue));
            case "budget" -> singleToken(cleanedParam, normalizeBudget(cleanedValue));
            case "weather" -> singleToken(cleanedParam, normalizeWeather(cleanedValue));
            case "companion" -> normalizeCompanion(cleanedParam, cleanedValue);
            case "taste" -> normalizeTaste(cleanedParam, cleanedValue);
            case "mood" -> normalizeMood(cleanedParam, cleanedValue);
            case "location" -> normalizeLocation(cleanedParam, cleanedValue);
            default -> List.of();
        };
    }

    /**
     * 统一生成单值 token。
     */
    private List<String> singleToken(String paramName, String normalizedValue) {
        if (normalizedValue == null || normalizedValue.isBlank()) {
            return List.of();
        }
        return List.of(paramName + "=" + normalizedValue);
    }

    /**
     * 将用餐时间归一为固定时段标签。
     */
    private String normalizeTime(String value) {
        if (containsAny(value, "夜宵", "宵夜", "今晚", "夜里", "半夜", "晚上", "夜间")) {
            return "night";
        }
        if (containsAny(value, "早餐", "早上", "清晨", "一早")) {
            return "breakfast";
        }
        if (containsAny(value, "午饭", "中午", "午餐")) {
            return "lunch";
        }
        if (containsAny(value, "下午茶", "下午")) {
            return "afternoon";
        }
        if (containsAny(value, "晚饭", "晚餐", "傍晚")) {
            return "dinner";
        }
        return null;
    }

    /**
     * 将预算文本映射到低中高区间。
     */
    private String normalizeBudget(String value) {
        if (containsAny(value, "便宜", "省", "实惠", "平价", "学生党", "不贵")) {
            return "low";
        }
        if (containsAny(value, "适中", "一般", "中等", "正常预算", "还行")) {
            return "medium";
        }
        if (containsAny(value, "贵", "贵点", "奢侈", "大餐", "高级", "预算充足")) {
            return "high";
        }

        Matcher matcher = NUMBER_PATTERN.matcher(value);
        if (!matcher.find()) {
            return null;
        }

        int amount = Integer.parseInt(matcher.group(1));
        if (amount <= 50) {
            return "low";
        }
        if (amount <= 150) {
            return "medium";
        }
        return "high";
    }

    /**
     * 将天气描述映射为稳定标签。
     */
    private String normalizeWeather(String value) {
        if (containsAny(value, "下雨", "雨天", "雨", "潮湿")) {
            return "rainy";
        }
        if (containsAny(value, "太阳", "晴", "晴朗", "阳光")) {
            return "sunny";
        }
        if (containsAny(value, "冷", "寒", "降温", "冬天")) {
            return "cold";
        }
        if (containsAny(value, "热", "炎热", "闷", "夏天")) {
            return "hot";
        }
        return null;
    }

    /**
     * 同行人允许多标签，但仍限制在固定集合内。
     */
    private List<String> normalizeCompanion(String paramName, String value) {
        Set<String> tokens = new LinkedHashSet<>();
        if (containsAny(value, "一个人", "独自", "单人", "自己")) {
            tokens.add(paramName + "=solo");
        }
        if (containsAny(value, "朋友", "同学", "闺蜜", "兄弟")) {
            tokens.add(paramName + "=friends");
        }
        if (containsAny(value, "家人", "爸妈", "孩子", "家庭")) {
            tokens.add(paramName + "=family");
        }
        if (containsAny(value, "对象", "情侣", "约会", "男朋友", "女朋友")) {
            tokens.add(paramName + "=partner");
        }
        if (containsAny(value, "同事", "公司", "团队", "领导")) {
            tokens.add(paramName + "=coworkers");
        }
        return sortedTokens(tokens);
    }

    /**
     * 将高频口味和品类描述映射为稳定标签。
     */
    private List<String> normalizeTaste(String paramName, String value) {
        Set<String> tokens = new LinkedHashSet<>();
        if (containsAny(value, "辣", "麻辣", "香辣", "重辣", "辣一点")) {
            tokens.add(paramName + "=spicy");
        }
        if (containsAny(value, "甜", "奶油", "蛋糕", "甜品")) {
            tokens.add(paramName + "=sweet");
        }
        if (containsAny(value, "酸", "酸辣", "酸甜")) {
            tokens.add(paramName + "=sour");
        }
        if (containsAny(value, "清淡", "少油", "养生", "不腻")) {
            tokens.add(paramName + "=light");
        }
        if (containsAny(value, "重口", "浓郁", "顶饱", "下饭")) {
            tokens.add(paramName + "=heavy");
        }
        if (containsAny(value, "火锅", "锅子", "毛肚", "涮")) {
            tokens.add(paramName + "=hotpot");
        }
        if (containsAny(value, "烧烤", "烤串", "烤肉", "串串")) {
            tokens.add(paramName + "=bbq");
        }
        if (containsAny(value, "面", "面条", "拉面", "粉面")) {
            tokens.add(paramName + "=noodle");
        }
        if (containsAny(value, "饭", "盖饭", "米饭", "炒饭")) {
            tokens.add(paramName + "=rice");
        }
        return sortedTokens(tokens);
    }

    /**
     * 将用户情绪与用餐诉求映射为稳定标签。
     */
    private List<String> normalizeMood(String paramName, String value) {
        Set<String> tokens = new LinkedHashSet<>();
        if (containsAny(value, "治愈", "安慰", "暖胃", "放松", "抚慰")) {
            tokens.add(paramName + "=comforting");
        }
        if (containsAny(value, "庆祝", "开心", "聚会", "生日", "犒劳")) {
            tokens.add(paramName + "=celebration");
        }
        if (containsAny(value, "累", "没精神", "困", "疲惫", "不想动")) {
            tokens.add(paramName + "=low_energy");
        }
        if (containsAny(value, "尝鲜", "刺激", "特别点", "冒险", "新鲜")) {
            tokens.add(paramName + "=adventurous");
        }
        if (containsAny(value, "快一点", "赶时间", "来不及", "方便", "快餐")) {
            tokens.add(paramName + "=quick_meal");
        }
        return sortedTokens(tokens);
    }

    /**
     * 将常见地点场景映射为稳定标签。
     */
    private List<String> normalizeLocation(String paramName, String value) {
        Set<String> tokens = new LinkedHashSet<>();
        if (containsAny(value, "家", "回家", "家里", "宿舍")) {
            tokens.add(paramName + "=home");
        }
        if (containsAny(value, "公司", "办公室", "写字楼", "上班")) {
            tokens.add(paramName + "=office");
        }
        if (containsAny(value, "学校", "教室", "食堂", "校园")) {
            tokens.add(paramName + "=school");
        }
        if (containsAny(value, "商场", "mall", "购物中心")) {
            tokens.add(paramName + "=mall");
        }
        if (containsAny(value, "路边", "街边", "街上", "附近")) {
            tokens.add(paramName + "=street");
        }
        if (containsAny(value, "地铁", "车站", "通勤")) {
            tokens.add(paramName + "=subway");
        }
        return sortedTokens(tokens);
    }

    /**
     * 判断文本是否包含任一候选关键词。
     */
    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 统一输出排序后的 token 列表，避免规则顺序影响匹配稳定性。
     */
    private List<String> sortedTokens(Set<String> tokens) {
        return tokens.stream().sorted(Comparator.naturalOrder()).toList();
    }
}
