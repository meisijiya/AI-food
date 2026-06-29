package com.aifood.admin.controller;

import com.ai.food.common.model.ConversationSession;
import com.ai.food.common.model.QaRecord;
import com.ai.food.common.util.ApiResponse;
import com.aifood.admin.common.annotation.AuditLog;
import com.aifood.admin.common.annotation.RequireAdmin;
import com.aifood.admin.dto.ConversationQueryReq;
import com.aifood.admin.service.ConversationService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台 AI 对话会话管理接口。
 *
 * <p>4 个接口:
 * <ul>
 *   <li>GET    /admin/api/conversations              分页列表</li>
 *   <li>GET    /admin/api/conversations/{id}         详情</li>
 *   <li>GET    /admin/api/conversations/{id}/messages 分页问答记录</li>
 *   <li>DELETE /admin/api/conversations/{id}         软删除(审计:DELETE_CONVERSATION)</li>
 * </ul>
 *
 * <p>所有接口由 {@link com.aifood.admin.common.AdminWebConfig} 拦截器统一鉴权
 * （除 /login 外 /admin/api/** 全部要求 ADMIN token 有效）。类级别 {@link RequireAdmin}
 * 是冗余防御层 —— 拦截器已生效,这里同时保留以与 AuthController / UserController 风格一致。</p>
 */
@RestController
@RequestMapping("/admin/api/conversations")
@RequireAdmin
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    /** 分页查询会话列表 */
    @GetMapping
    public ApiResponse<Page<ConversationSession>> list(ConversationQueryReq req) {
        return ApiResponse.success(conversationService.page(req));
    }

    /** 查询单个会话详情 */
    @GetMapping("/{id}")
    public ApiResponse<ConversationSession> detail(@PathVariable Long id) {
        return ApiResponse.success(conversationService.getDetail(id));
    }

    /**
     * 分页查询会话下的问答记录。
     *
     * @param id   会话主键
     * @param page 页码,默认 1
     * @param size 每页条数,默认 50
     */
    @GetMapping("/{id}/messages")
    public ApiResponse<Page<QaRecord>> messages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.success(conversationService.getMessages(id, page, size));
    }

    /** 软删除会话 (审计:DELETE_CONVERSATION) */
    @DeleteMapping("/{id}")
    @AuditLog(value = "删除对话", action = "DELETE_CONVERSATION")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        conversationService.delete(id);
        return ApiResponse.success();
    }
}
