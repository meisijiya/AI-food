package com.aifood.admin.dto;

import lombok.Data;

/**
 * 用户列表分页查询请求。
 * <ul>
 *   <li>keyword: 模糊匹配 username / email / nickname</li>
 *   <li>role: 精确匹配 USER 或 ADMIN</li>
 *   <li>status: 0=启用 1=禁用（对应 sys_user.is_deleted）</li>
 *   <li>startDate / endDate: yyyy-MM-dd 或 yyyy-MM-ddTHH:mm:ss(包含边界)</li>
 * </ul>
 */
@Data
public class UserQueryReq {
    /** 页码,默认 1 */
    private Integer page = 1;
    /** 每页条数,默认 20 */
    private Integer size = 20;
    /** 模糊匹配 username/email/nickname */
    private String keyword;
    /** 精确匹配 USER | ADMIN */
    private String role;
    /** 0 启用,1 禁用 */
    private Integer status;
    /** 起始日期 yyyy-MM-dd 或完整 LocalDateTime */
    private String startDate;
    /** 截止日期 yyyy-MM-dd 或完整 LocalDateTime */
    private String endDate;
}