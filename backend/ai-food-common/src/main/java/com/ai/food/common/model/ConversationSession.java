package com.ai.food.common.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 对话会话实体（MyBatis-Plus 迁移版）
 * <p>
 * 表名 conversation_session。索引/约束由 Flyway 迁移脚本管理。
 */
@Data
@TableName("conversation_session")
public class ConversationSession {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("session_id")
    private String sessionId;

    @TableField("user_id")
    private Long userId;

    @TableField("total_questions")
    private Integer totalQuestions;

    @TableField("current_question_count")
    private Integer currentQuestionCount = 0;

    @TableField("interrupt_count")
    private Integer interruptCount = 0;

    private String mode; // inertia/random

    private String status = "active";

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField("is_deleted")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted = 0;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;

    @TableField("version")
    @Version
    private Integer version;
}
