package com.ai.food.service.match;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("RuleBasedParamNormalizer 规则标准化")
class RuleBasedParamNormalizerTest {

    private final RuleBasedParamNormalizer normalizer = new RuleBasedParamNormalizer();

    @Test
    @DisplayName("时间参数按规则映射到统一标签")
    void normalize_timeValue_mapsToStableToken() {
        List<String> tokens = normalizer.normalize("time", "今晚想吃点热乎的");

        assertEquals(List.of("time=night"), tokens);
    }

    @Test
    @DisplayName("预算参数按规则映射到统一标签")
    void normalize_budgetValue_mapsToBucket() {
        List<String> tokens = normalizer.normalize("budget", "30块以内，便宜点");

        assertEquals(List.of("budget=low"), tokens);
    }

    @Test
    @DisplayName("口味参数可直接命中高频规则标签")
    void normalize_tasteValue_mapsToStableTasteTokens() {
        List<String> tokens = normalizer.normalize("taste", "想吃重辣一点，最好是火锅");

        assertEquals(List.of("taste=hotpot", "taste=spicy"), tokens);
    }

    @Test
    @DisplayName("心情参数可直接命中场景规则标签")
    void normalize_moodValue_mapsToStableMoodTokens() {
        List<String> tokens = normalizer.normalize("mood", "今天有点累，想吃点治愈的，顺便快一点");

        assertEquals(List.of("mood=comforting", "mood=low_energy", "mood=quick_meal"), tokens);
    }

    @Test
    @DisplayName("地点参数可直接命中常见场景标签")
    void normalize_locationValue_mapsToStableLocationTokens() {
        List<String> tokens = normalizer.normalize("location", "还在公司，晚点回家");

        assertEquals(List.of("location=home", "location=office"), tokens);
    }
}
