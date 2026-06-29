package com.aifood.admin.service;

import com.ai.food.common.mapper.UserMapper;
import com.ai.food.common.model.SysUser;
import com.aifood.admin.dto.DashboardSummaryVO;
import com.aifood.admin.dto.TrendVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 管理后台 Dashboard 数据聚合服务。
 *
 * <p>用户相关指标直接查 sys_user;会话/Token 指标 Phase 2 暂返回 0,
 * Phase 3 接入 ChatConversationMapper / QaRecordMapper 后填充。</p>
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    /** 用户表 Mapper */
    private final UserMapper userMapper;
    /** Redis 操作模板,用于统计在线 token */
    private final StringRedisTemplate redisTemplate;

    /**
     * 汇总首页所有指标。
     *
     * @return DashboardSummaryVO,userCount 来自 sys_user 全表计数,
     *         todayNew 为当日 created_at 落入 [00:00, 23:59:59.999...] 的用户数,
     *         其余字段 Phase 2 占位 0,Phase 3 接入会话/Token mapper 后填充。
     */
    public DashboardSummaryVO summary() {
        DashboardSummaryVO vo = new DashboardSummaryVO();
        // 全表计数(selectCount 会自动追加 is_deleted=0,因为 SysUser 上标了 @TableLogic)
        vo.setUserCount(userMapper.selectCount(null));

        // 今日新增:created_at 落入今天的 [00:00:00, 23:59:59.999...]
        LocalDateTime start = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        w.between(SysUser::getCreatedAt, start, end);
        vo.setTodayNew(userMapper.selectCount(w));

        // ponytail: Phase 2 占位,Phase 3 接 ChatConversationMapper / QaRecordMapper 后替换
        vo.setConversationCount(0L);
        vo.setTodayConversations(0L);
        vo.setTokenToday(0L);
        vo.setTokenMonthTotal(0L);

        // ponytail: KEYS 适合低基数 token 场景(全局并发 < 万级),
        // 若后期 QPS/在线数上升,改为 SCAN cursor 迭代避免阻塞 Redis
        Set<String> keys = redisTemplate.keys("token:*");
        vo.setOnlineCount(keys != null ? (long) keys.size() : 0L);

        Map<String, String> health = new HashMap<>();
        health.put("jvm", "UP");
        health.put("db", "UP");
        health.put("redis", "UP");
        vo.setSystemHealth(health);
        return vo;
    }

    /**
     * 计算最近 N 天用户新增趋势与(占位)会话趋势。
     *
     * <p>按日期升序返回,日期从 today - (days-1) 到 today。</p>
     *
     * @param days 趋势天数,默认 7
     * @return Map key 为 "userTrend" / "conversationTrend",
     *         value 为对应每天的 (date, count) 列表。
     *         会话趋势 Phase 2 全部置 0。
     */
    public Map<String, List<TrendVO>> trends(int days) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate today = LocalDate.now();
        Map<String, List<TrendVO>> r = new HashMap<>();
        r.put("userTrend", new ArrayList<>());
        r.put("conversationTrend", new ArrayList<>());

        // 从最早到今天,逐日查 sys_user 创建数
        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            LocalDateTime s = LocalDateTime.of(day, LocalTime.MIN);
            LocalDateTime e = LocalDateTime.of(day, LocalTime.MAX);
            String label = day.format(fmt);
            LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
            w.between(SysUser::getCreatedAt, s, e);
            long count = userMapper.selectCount(w);

            TrendVO user = new TrendVO();
            user.setDate(label);
            user.setCount(count);
            r.get("userTrend").add(user);

            // ponytail: conversationTrend 占位 0,Phase 3 接 ChatConversationMapper 后替换
            TrendVO conv = new TrendVO();
            conv.setDate(label);
            conv.setCount(0L);
            r.get("conversationTrend").add(conv);
        }
        return r;
    }
}
