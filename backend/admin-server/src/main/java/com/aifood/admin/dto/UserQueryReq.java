package com.aifood.admin.dto;

import lombok.Data;

/**
 * 用户列表分页查询请求。
 * <ul>
 *   <li>keyword: 模糊匹配 username / email / nickname</li>
 *   <li>role: 精确匹配 USER 或 ADMIN</li>
 *   <li>status: 0=启用 1=禁用（对应 sys_user.is_deleted）</li>
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
}
