package com.ai.food.common.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("photo")
public class Photo {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("original_path")
    private String originalPath;

    @TableField("thumbnail_path")
    private String thumbnailPath;

    @TableField("related_session_id")
    private String relatedSessionId;

    @TableField("file_name")
    private String fileName;

    @TableField("original_size")
    private Long originalSize;

    @TableField("thumbnail_size")
    private Long thumbnailSize;

    @TableField("mime_type")
    private String mimeType;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("is_deleted")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted = 0;

    @TableField("version")
    @Version
    private Integer version;
}
