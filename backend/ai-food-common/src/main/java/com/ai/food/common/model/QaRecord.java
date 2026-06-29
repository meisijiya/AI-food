package com.ai.food.common.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * QA 问答记录实体（MyBatis-Plus 迁移版）
 * <p>
 * 表名 qa_record。索引 idx_session 由 Flyway 迁移脚本管理。
 * <p>
 * 注意：{@code isValid} 是普通业务字段（问答是否有效），不是软删除标志；
 *       {@code isDeleted} 才是软删除字段，配合 MP {@link TableLogic} 自动生成 WHERE 子句。
 */
@Data
@TableName("qa_record")
public class QaRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("session_id")
    private String sessionId;

    @TableField("question_type")
    private String questionType; // question/chat/2question/interrupt

    @TableField("param_name")
    private String paramName;

    @TableField("ai_question")
    private String aiQuestion;

    @TableField("user_answer")
    private String userAnswer;

    /**
     * 业务字段：问答是否有效。普通 boolean，**非**软删除字段，不加 @TableLogic。
     */
    @TableField("is_valid")
    private Boolean isValid = true;

    @TableField("question_order")
    private Integer questionOrder;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("is_deleted")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted = 0;

    @TableField("version")
    @Version
    private Integer version;

    /** LLM 提示词 token 数（仅 LLM 生成问答时填写，intake Q&A 留 null） */
    @TableField("prompt_tokens")
    private Integer promptTokens;

    /** LLM 完成 token 数（仅 LLM 生成问答时填写，intake Q&A 留 null） */
    @TableField("completion_tokens")
    private Integer completionTokens;

    /** 总 token 数（仅 LLM 生成问答时填写，intake Q&A 留 null） */
    @TableField("total_tokens")
    private Integer totalTokens;

    /** 调用的模型名（如 qwen-turbo / gpt-4o-mini） */
    @TableField("model")
    private String model;
}
