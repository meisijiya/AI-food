package com.ai.food.service.match;

import com.ai.food.service.ai.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 当规则无法识别时，使用 AI 将自由文本压缩到白名单标签集合中。
 */
@Slf4j
@Component
public class AiParamNormalizer {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("([a-z_]+=[a-z_]+)");

    private static final Map<String, Set<String>> ALLOWED_VALUES = Map.of(
            "taste", Set.of("spicy", "sweet", "sour", "light", "heavy", "hotpot", "bbq", "noodle", "rice"),
            "mood", Set.of("comforting", "celebration", "low_energy", "adventurous", "quick_meal"),
            "location", Set.of("home", "office", "school", "mall", "street", "subway")
    );

    private final AiService aiService;

    /**
     * 显式构造器用于稳定测试与运行时注入。
     */
    public AiParamNormalizer(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * 使用 AI 生成白名单内的标准 token；失败时返回空列表。
     */
    public List<String> normalize(String paramName, String rawValue) {
        if (paramName == null || rawValue == null) {
            return List.of();
        }

        String cleanedParam = paramName.trim().toLowerCase(Locale.ROOT);
        String cleanedValue = rawValue.trim();
        Set<String> allowedValues = ALLOWED_VALUES.get(cleanedParam);
        if (allowedValues == null || cleanedValue.isBlank()) {
            return List.of();
        }

        try {
            String systemPrompt = "你是一个参数标准化助手，只能输出白名单里的 token。";
            String userPrompt = String.format(
                    Locale.ROOT,
                    "参数名：%s\n用户原话：%s\n允许标签：%s\n请只返回逗号分隔的 token，格式必须是 %s=标签。",
                    cleanedParam,
                    cleanedValue,
                    String.join(",", allowedValues.stream().sorted().toList()),
                    cleanedParam
            );
            String result = aiService.chat(systemPrompt, userPrompt);
            return extractAllowedTokens(cleanedParam, result, allowedValues);
        } catch (Exception ex) {
            log.warn("AI param normalization failed: paramName={}, error={}", cleanedParam, ex.getMessage());
            return List.of();
        }
    }

    /**
     * 从 AI 响应里提取并过滤掉白名单之外的 token。
     */
    private List<String> extractAllowedTokens(String paramName, String response, Set<String> allowedValues) {
        if (response == null || response.isBlank()) {
            return List.of();
        }

        Set<String> tokens = new LinkedHashSet<>();
        Matcher matcher = TOKEN_PATTERN.matcher(response.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String token = matcher.group(1);
            String[] parts = token.split("=", 2);
            if (parts.length != 2 || !paramName.equals(parts[0])) {
                continue;
            }
            if (allowedValues.contains(parts[1])) {
                tokens.add(token);
            }
        }

        List<String> result = new ArrayList<>(tokens);
        result.sort(Comparator.naturalOrder());
        return result;
    }
}
