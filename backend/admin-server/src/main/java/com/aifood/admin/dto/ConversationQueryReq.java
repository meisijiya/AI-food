package com.aifood.admin.dto;

import lombok.Data;

/**
 * AI 对话会话分页查询请求。
 *
 * <p>支持按用户、会话模式、会话状态、时间范围过滤。注意：本 DTO 的字段是会话（{@code conversation_session}）级别的
 * 过滤项；模型名（{@code model}）是问答记录（{@code qa_record}）的字段,需要通过子查询或 JOIN
 * 才能在会话列表上过滤,本期未实现,后续如需要再加。</p>
 *
 * <ul>
 *   <li>page / size : 分页,默认 1 / 20</li>
 *   <li>userId      : 精确匹配 conversation_session.user_id</li>
 *   <li>mode        : 精确匹配 inertia / random（推荐模式）</li>
 *   <li>status      : 精确匹配 active / completed</li>
 *   <li>startDate / endDate : created_at 闭区间,ISO yyyy-MM-dd</li>
 * </ul>
 */
@Data
public class ConversationQueryReq {
    /** 页码,默认 1 */
    private Integer page = 1;
    /** 每页条数,默认 20 */
    private Integer size = 20;
    /** 按用户过滤 */
    private Long userId;
    /** 按会话模式过滤:inertia / random */
    private String mode;
    /** 按会话状态过滤:active / completed */
    private String status;
    /** 起始日期(含),格式 yyyy-MM-dd */
    private String startDate;
    /** 截止日期(含),格式 yyyy-MM-dd */
    private String endDate;
}
