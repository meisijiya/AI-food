package com.aifood.admin.controller;

import com.ai.food.common.util.ApiResponse;
import com.aifood.admin.common.annotation.RequireAdmin;
import com.aifood.admin.common.audit.AuditLogEntity;
import com.aifood.admin.common.audit.AuditLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台审计日志查询接口。
 *
 * <p>单一端点:
 * <ul>
 *   <li>GET /admin/api/audit-logs?page=&size=&actorId=&action=&startDate=&endDate=</li>
 * </ul>
 *
 * <p>日期参数格式 yyyy-MM-dd(包含边界);MyBatis-Plus 将字符串直接绑定到 DATETIME 列,
 * 依赖 MySQL 字符串字典序 == 时间顺序的隐式行为。</p>
 *
 * <p>由 {@link com.aifood.admin.common.AdminWebConfig} 拦截器统一鉴权,
 * 类级 {@link RequireAdmin} 作为冗余防御。</p>
 */
@RestController
@RequestMapping("/admin/api/audit-logs")
@RequireAdmin
@RequiredArgsConstructor
public class AuditLogController {

    /** 审计日志 MyBatis-Plus Mapper */
    private final AuditLogMapper auditLogMapper;

    /**
     * 分页查询审计日志,按 created_at 倒序。
     *
     * @param page      页码,默认 1
     * @param size      页大小,默认 50
     * @param actorId   操作者 id 可选过滤
     * @param action    动作类型(LOGIN / UPDATE_USER 等)可选过滤
     * @param startDate 起始日期 yyyy-MM-dd 包含
     * @param endDate   截止日期 yyyy-MM-dd 包含
     * @return 分页结果
     */
    @GetMapping
    public ApiResponse<Page<AuditLogEntity>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Page<AuditLogEntity> p = new Page<>(page, size);
        LambdaQueryWrapper<AuditLogEntity> w = new LambdaQueryWrapper<>();
        if (actorId != null) w.eq(AuditLogEntity::getActorId, actorId);
        if (StringUtils.hasText(action)) w.eq(AuditLogEntity::getAction, action);
        if (StringUtils.hasText(startDate)) w.ge(AuditLogEntity::getCreatedAt, startDate);
        if (StringUtils.hasText(endDate)) w.le(AuditLogEntity::getCreatedAt, endDate);
        w.orderByDesc(AuditLogEntity::getCreatedAt);
        return ApiResponse.success(auditLogMapper.selectPage(p, w));
    }
}
