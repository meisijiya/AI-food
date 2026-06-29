package com.ai.food.common.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天消息实体（MyBatis-Plus）
 * <p>
 * 表名 chat_message。索引由 Flyway 迁移脚本管理。
 * <p>
 * 注意：{@code isRead} 是普通业务字段（消息已读），不是软删除标志；
 *       {@code isDeleted} 才是软删除字段，配合 MP {@link TableLogic} 自动生成 WHERE 子句。
 */
@Data
@TableName("chat_message")
public class ChatMessage {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("conversation_id")
    private Long conversationId;

    @TableField("sender_id")
    private Long senderId;

    @TableField("receiver_id")
    private Long receiverId;

    @TableField("content")
    private String content;

    @TableField("message_type")
    private String messageType = "text";

    @TableField("photo_id")
    private Long photoId;

    @TableField("file_id")
    private Long fileId;

    /**
     * 业务字段：消息是否已读。普通 boolean，**非**软删除字段，不加 @TableLogic。
     */
    @TableField("is_read")
    private Boolean isRead = false;

    /**
     * 软删除字段：0=未删，1=已删。{@link TableLogic} 自动处理 WHERE 与 delete 翻译。
     */
    @TableField("is_deleted")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted = 0;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @Version
    @TableField("version")
    private Integer version;
}
