package com.ai.food.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天会话实体（MyBatis-Plus）
 * <p>
 * 表名 chat_conversation。索引由 Flyway 迁移脚本管理。
 */
@Data
@TableName("chat_conversation")
public class ChatConversation {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("conversation_key")
    private String conversationKey;

    @TableField("user1_id")
    private Long user1Id;

    @TableField("user2_id")
    private Long user2Id;

    @TableField("last_message")
    private String lastMessage;

    @TableField("last_message_at")
    private LocalDateTime lastMessageAt;

    @TableField("cleared_at_user1")
    private LocalDateTime clearedAtUser1;

    @TableField("cleared_at_user2")
    private LocalDateTime clearedAtUser2;

    @TableField("hidden_at_user1")
    private LocalDateTime hiddenAtUser1;

    @TableField("hidden_at_user2")
    private LocalDateTime hiddenAtUser2;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @Version
    @TableField("version")
    private Integer version;

    /**
     * 按两个 userId 排序生成会话唯一 key（smaller_larger）。
     *
     * @param userId1 任意一方 userId
     * @param userId2 另一方 userId
     * @return 形如 "smallerId_largerId" 的会话 key
     */
    public static String generateKey(Long userId1, Long userId2) {
        long min = Math.min(userId1, userId2);
        long max = Math.max(userId1, userId2);
        return min + "_" + max;
    }
}
