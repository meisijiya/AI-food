package com.ai.food.common.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_token_quota")
public class UserTokenQuota {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("daily_token_limit")
    private Integer dailyTokenLimit;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("version")
    @Version
    private Integer version;
}
