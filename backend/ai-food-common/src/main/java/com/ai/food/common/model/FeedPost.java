package com.ai.food.common.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("feed_post")
public class FeedPost {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("session_id")
    private String sessionId;

    @TableField("food_name")
    private String foodName;

    @TableField("comment_preview")
    private String commentPreview;

    @TableField("thumbnail_url")
    private String thumbnailUrl;

    @TableField("original_photo_url")
    private String originalPhotoUrl;

    @TableField("reason")
    private String reason;

    @TableField("collected_params")
    private String collectedParams;

    @TableField("like_count")
    private Integer likeCount = 0;

    @TableField("comment_count")
    private Integer commentCount = 0;

    @TableField("view_count")
    private Integer viewCount = 0;

    @TableField("visibility")
    private String visibility = "public";

    @TableField("published_at")
    private LocalDateTime publishedAt;

    @TableField("is_deleted")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted = 0;

    @TableField("version")
    @Version
    private Integer version;
}
