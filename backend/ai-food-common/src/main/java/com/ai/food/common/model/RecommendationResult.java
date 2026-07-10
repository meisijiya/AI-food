package com.ai.food.common.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
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

    @TableField("food_name")
    private String foodName;

    private String reason;

    @TableField("category")
    private String category;

    @TableField("flavor_tags")
    private String flavorTags;

    @TableField("total_tokens")
    private Integer totalTokens;

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
