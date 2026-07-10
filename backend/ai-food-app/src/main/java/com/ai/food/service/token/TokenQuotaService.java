package com.ai.food.service.token;

import com.ai.food.common.mapper.QaRecordMapper;
import com.ai.food.common.mapper.SystemConfigMapper;
import com.ai.food.common.mapper.UserTokenQuotaMapper;
import com.ai.food.common.model.QaRecord;
import com.ai.food.common.model.SystemConfig;
import com.ai.food.common.model.UserTokenQuota;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenQuotaService {

    private static final String DEFAULT_DAILY_LIMIT_KEY = "daily_token_limit_default";
    private static final int HARDCODED_DEFAULT_LIMIT = 1_000_000;

    private final QaRecordMapper qaRecordMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final UserTokenQuotaMapper userTokenQuotaMapper;

    // P1-2 修订: 注入 Clock 支持跨日测试（@RequiredArgsConstructor 构造注入）
    private final Clock clock;

    /**
     * 查有效限额（per-user 覆盖 > 全局 > 默认 1M）
     */
    public int getEffectiveLimit(long userId) {
        // 1. per-user 覆盖
        UserTokenQuota userQuota = userTokenQuotaMapper.findByUserId(userId);
        if (userQuota != null && userQuota.getDailyTokenLimit() != null) {
            return userQuota.getDailyTokenLimit();
        }
        // 2. 全局配置
        SystemConfig config = systemConfigMapper.findByKey(DEFAULT_DAILY_LIMIT_KEY);
        if (config != null && config.getConfigValue() != null) {
            try {
                return Integer.parseInt(config.getConfigValue());
            } catch (NumberFormatException e) {
                log.warn("Invalid global config value: {}", config.getConfigValue());
            }
        }
        // 3. 硬编码默认
        return HARDCODED_DEFAULT_LIMIT;
    }

    /**
     * 查今日已用 token（P1-2 修订: 用 LocalDate.now(clock) 支持测试注入）
     */
    public long getTodayUsed(long userId) {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        QueryWrapper<QaRecord> w = new QueryWrapper<>();
        w.eq("user_id", userId);
        w.isNotNull("total_tokens");
        w.between("created_at", startOfDay, endOfDay);
        w.select("SUM(total_tokens) AS total");

        // MyBatis-Plus selectMaps 返回 Map; SUM 结果可能为 null
        java.util.List<java.util.Map<String, Object>> result = qaRecordMapper.selectMaps(w);
        if (result == null || result.isEmpty()) return 0L;
        Object total = result.get(0).get("total");
        if (total == null) return 0L;
        return ((Number) total).longValue();
    }

    /**
     * 检查并拒绝（返回 null=放行 / 错误消息=拒绝）
     */
    public String checkAndReject(long userId, int estimatedTokens) {
        int limit = getEffectiveLimit(userId);
        long used = getTodayUsed(userId);
        if (used + estimatedTokens > limit) {
            return String.format("今日 token 额度已用完（已用 %d，预计用 %d，限额 %d）", used, estimatedTokens, limit);
        }
        return null;
    }

    /**
     * 累加 token 用量（每次 AI 调用后调）
     */
    public void recordUsage(long userId, int promptTokens, int completionTokens, int totalTokens,
                            String model, String sessionId, String paramName) {
        try {
            QaRecord record = new QaRecord();
            record.setUserId(userId);
            record.setSessionId(sessionId);
            record.setParamName(paramName);
            record.setQuestionType("ai_call");
            record.setQuestionOrder(0);  // AI call 不是 Q&A
            record.setPromptTokens(promptTokens);
            record.setCompletionTokens(completionTokens);
            record.setTotalTokens(totalTokens);
            record.setModel(model);
            record.setCreatedAt(LocalDateTime.now(clock));
            record.setIsValid(true);
            record.setIsDeleted(0);
            record.setVersion(0);
            qaRecordMapper.insert(record);
        } catch (Exception e) {
            log.warn("Failed to record token usage: {}", e.getMessage());
        }
    }
}
