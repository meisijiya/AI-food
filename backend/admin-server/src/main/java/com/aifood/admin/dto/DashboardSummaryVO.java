package com.aifood.admin.dto;

import lombok.Data;
import java.util.Map;

/**
 * Dashboard 概览视图对象。
 *
 * <p>聚合用户、会话、Token、在线状态与系统健康度等核心指标,
 * 供管理后台首页一次性渲染。</p>
 */
@Data
public class DashboardSummaryVO {
    /** 总用户数(未软删) */
    private Long userCount;
    /** 今日新增用户数 */
    private Long todayNew;
    /** 总会话数 */
    private Long conversationCount;
    /** 今日会话数 */
    private Long todayConversations;
    /** 今日 Token 消耗 */
    private Long tokenToday;
    /** 本月 Token 总消耗 */
    private Long tokenMonthTotal;
    /** 当前在线 token 数(基于 Redis key 数估算) */
    private Long onlineCount;
    /** 系统健康度 {jvm/db/redis: UP|DOWN} */
    private Map<String, String> systemHealth;
}
