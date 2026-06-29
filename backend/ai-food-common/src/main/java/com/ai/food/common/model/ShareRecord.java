package com.ai.food.common.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("share_record")
public class ShareRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("share_token")
    private String shareToken;

    @TableField("user_id")
    private Long userId;

    @TableField("session_id")
    private String sessionId;

    @TableField("view_count")
    private Integer viewCount = 0;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("is_deleted")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted = 0;

    @TableField("version")
    @Version
    private Integer version;
}
