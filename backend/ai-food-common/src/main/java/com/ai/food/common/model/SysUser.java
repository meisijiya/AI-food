package com.ai.food.common.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(value = "username", condition = "%s LIKE #{%s}")
    private String username;

    private String password;

    private String email;

    private String nickname;

    private String avatar;

    /** 用户角色：USER | ADMIN。由 V4 migration 添加,默认 USER。 */
    private String role;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableField("is_deleted")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    @TableField("version")
    @Version
    private Integer version;
}
