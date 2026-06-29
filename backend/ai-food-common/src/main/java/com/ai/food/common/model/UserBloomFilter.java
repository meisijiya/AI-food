package com.ai.food.common.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_bloom_filter")
public class UserBloomFilter {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("bit_array")
    private byte[] bitArray;

    @TableField("record_count")
    private Integer recordCount = 0;

    @TableField("last_record_id")
    private String lastRecordId;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("is_deleted")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    @TableField("version")
    @Version
    private Integer version;
}
