package com.aifood.admin.common.audit;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/** admin 操作审计日志实体 */
@Data
@TableName("admin_audit_log")
public class AuditLogEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long actorId;
    private String actorUsername;
    private String action;
    private String targetType;
    private String targetId;
    private String payload;
    private String ip;
    private String userAgent;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
}
