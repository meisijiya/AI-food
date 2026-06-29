package com.ai.food.service.match;

import com.ai.food.service.ai.AiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParamNormalizationService 混合标准化")
class ParamNormalizationServiceTest {

    @Mock
    private AiService aiService;

    @Test
    @DisplayName("规则命中时不调用 AI 兜底")
    void normalize_usesRuleBeforeAiFallback() {
        ParamNormalizationService service = new ParamNormalizationService(
                new RuleBasedParamNormalizer(),
                new AiParamNormalizer(aiService)
        );

        List<String> tokens = service.normalize("time", "夜宵时间");

        assertEquals(List.of("time=night"), tokens);
        verify(aiService, never()).chat(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("规则未命中时使用 AI 输出白名单标签")
    void normalize_usesAiFallbackWhenRuleMisses() {
        ParamNormalizationService service = new ParamNormalizationService(
                new RuleBasedParamNormalizer(),
                new AiParamNormalizer(aiService)
        );
        when(aiService.chat(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("taste=spicy,taste=hotpot,taste=free_text");

        List<String> tokens = service.normalize("taste", "想吃点刺激又带汤的");

        assertEquals(List.of("taste=hotpot", "taste=spicy"), tokens);
    }
}
