package com.ai.food.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 推荐结果实体（MyBatis-Plus 迁移版）
 * <p>
 * 表名 recommendation_result。索引 idx_session 由 Flyway 迁移脚本管理。
 */
@Data
@TableName("recommendation_result")
public class RecommendationResult {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("session_id")
    private String sessionId;

    private String mode;

    @TableField("food_name")
    private String foodName;

    @TableField("old_food")
    private String oldFood; // 随机模式的旧值

    @TableField("similarity_score")
    private BigDecimal similarityScore;

    private String reason;

    @TableField("photo_url")
    private String photoUrl;

    private String comment;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("is_deleted")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted = 0;

    @TableField("version")
    @Version
    private Integer version;
}
