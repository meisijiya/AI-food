package com.aifood.admin.dto;

import lombok.Data;

/**
 * 趋势图数据点:某一日 + 对应计数。
 * 用于 Dashboard 折线图 userTrend / conversationTrend。
 */
@Data
public class TrendVO {
    /** 日期字符串,格式 yyyy-MM-dd */
    private String date;
    /** 当日数量 */
    private Long count;
}
