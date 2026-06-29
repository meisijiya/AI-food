package com.ai.food.common.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 已收集参数实体（MyBatis-Plus 迁移版）
 * <p>
 * 表名 collected_params。唯一约束 (session_id, param_name) 由 Flyway 迁移脚本管理。
 */
@Data
@TableName("collected_params")
public class CollectedParam {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("session_id")
    private String sessionId;

    @TableField("param_name")
    private String paramName;

    @TableField("param_value")
    private String paramValue;

    @TableField("param_type")
    private String paramType; // required/optional

    @TableField("collected_at")
    private LocalDateTime collectedAt;

    @TableField("is_deleted")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted = 0;

    @TableField("version")
    @Version
    private Integer version;
}
