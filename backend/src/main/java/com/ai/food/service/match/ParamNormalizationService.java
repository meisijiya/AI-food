package com.ai.food.service.match;

import com.ai.food.model.CollectedParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 组合规则与 AI 兜底逻辑，为匹配画像生成稳定 token 集合。
 */
@Slf4j
@Service
public class ParamNormalizationService {

    private final RuleBasedParamNormalizer ruleBasedParamNormalizer;
    private final AiParamNormalizer aiParamNormalizer;

    /**
     * 显式构造器用于稳定测试与运行时注入。
     */
    public ParamNormalizationService(RuleBasedParamNormalizer ruleBasedParamNormalizer,
                                     AiParamNormalizer aiParamNormalizer) {
        this.ruleBasedParamNormalizer = ruleBasedParamNormalizer;
        this.aiParamNormalizer = aiParamNormalizer;
    }

    /**
     * 对单个参数执行规则优先、AI 兜底的标准化。
     */
    public List<String> normalize(String paramName, String rawValue) {
        List<String> ruleTokens = ruleBasedParamNormalizer.normalize(paramName, rawValue);
        if (!ruleTokens.isEmpty()) {
            log.debug("Param normalize hit rules: paramName={}, rawValue={}, tokens={}", paramName, rawValue, ruleTokens);
            return ruleTokens;
        }
        List<String> aiTokens = aiParamNormalizer.normalize(paramName, rawValue);
        log.debug("Param normalize used AI fallback: paramName={}, rawValue={}, tokens={}", paramName, rawValue, aiTokens);
        return aiTokens;
    }

    /**
     * 对整组已收集参数生成去重、排序后的稳定 token 列表。
     */
    public List<String> normalizeCollectedParams(List<CollectedParam> collectedParams) {
        if (collectedParams == null || collectedParams.isEmpty()) {
            log.debug("Param normalize collected params skipped: empty input");
            return List.of();
        }

        Set<String> tokens = new LinkedHashSet<>();
        for (CollectedParam param : collectedParams) {
            if (param == null) {
                continue;
            }
            tokens.addAll(normalize(param.getParamName(), param.getParamValue()));
        }
        List<String> sortedTokens = tokens.stream().sorted(Comparator.naturalOrder()).toList();
        log.debug("Param normalize collected params result: count={}, tokens={}", sortedTokens.size(), sortedTokens);
        return sortedTokens;
    }
}
