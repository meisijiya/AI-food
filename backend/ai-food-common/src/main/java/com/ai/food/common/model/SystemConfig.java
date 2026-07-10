package com.ai.food.common.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("system_config")
public class SystemConfig {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("config_key")
    private String configKey;

    @TableField("config_value")
    private String configValue;

    @TableField("description")
    private String description;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("version")
    @Version
    private Integer version;
}
