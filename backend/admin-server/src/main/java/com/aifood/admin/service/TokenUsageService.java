package com.aifood.admin.service;

import com.ai.food.common.mapper.QaRecordMapper;
import com.ai.food.common.model.QaRecord;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 管理后台 Token 用量统计服务。
 *
 * <p>从 qa_record 表聚合 prompt/completion/total token 用量,
 * 支持按 session / model / day 三种维度分组。</p>
 *
 * <p>注意:qa_record 没有 user_id 字段,只有 session_id;
 * "user" 分组模式下实际是按 session 分组,前端需自行关联
 * conversation_session.user_id 展示用户视角。</p>
 */
@Service
@RequiredArgsConstructor
public class TokenUsageService {

    /** QA 记录 Mapper,继承 BaseMapper<QaRecord> 提供 selectMaps() */
    private final QaRecordMapper qaRecordMapper;

    /**
     * 聚合 token 用量。
     *
     * @param groupBy   分组维度(user / model / day),大小写不敏感,默认 day
     * @param startDate 起始日期(yyyy-MM-dd,含),可选
     * @param endDate   截止日期(yyyy-MM-dd,含),可选
     * @return 每条记录 key=分组键值,含 promptTokens / completionTokens /
     *         totalTokens / count,按 totalTokens 降序
     */
    public List<Map<String, Object>> stats(String groupBy, String startDate, String endDate) {
        QueryWrapper<QaRecord> w = new QueryWrapper<>();
        String select;
        String group;

        if ("user".equalsIgnoreCase(groupBy)) {
            // ponytail: qa_record 没有 user_id 字段,只有 session_id;
            // TODO: 接入 userId 维度后改为按 conversation_session.user_id JOIN
            select = "session_id as `key`, SUM(prompt_tokens) as promptTokens, "
                   + "SUM(completion_tokens) as completionTokens, SUM(total_tokens) as totalTokens, "
                   + "COUNT(*) as count";
            group = "session_id";
        } else if ("model".equalsIgnoreCase(groupBy)) {
            select = "model as `key`, SUM(prompt_tokens) as promptTokens, "
                   + "SUM(completion_tokens) as completionTokens, SUM(total_tokens) as totalTokens, "
                   + "COUNT(*) as count";
            group = "model";
        } else {
            // ponytail: 默认按 created_at 日期聚合
            select = "DATE(created_at) as `key`, SUM(prompt_tokens) as promptTokens, "
                   + "SUM(completion_tokens) as completionTokens, SUM(total_tokens) as totalTokens, "
                   + "COUNT(*) as count";
            group = "DATE(created_at)";
        }

        w.select(select);
        // 只统计有 token 数据的记录(intake Q&A 不消耗 token)
        w.isNotNull("total_tokens");

        if (startDate != null && !startDate.isEmpty()) {
            LocalDateTime s = LocalDateTime.of(
                    LocalDate.parse(startDate, DateTimeFormatter.ISO_DATE), LocalTime.MIN);
            w.ge("created_at", s);
        }
        if (endDate != null && !endDate.isEmpty()) {
            LocalDateTime e = LocalDateTime.of(
                    LocalDate.parse(endDate, DateTimeFormatter.ISO_DATE), LocalTime.MAX);
            w.le("created_at", e);
        }

        w.groupBy(group);
        w.orderByDesc("totalTokens");

        return qaRecordMapper.selectMaps(w);
    }
}
