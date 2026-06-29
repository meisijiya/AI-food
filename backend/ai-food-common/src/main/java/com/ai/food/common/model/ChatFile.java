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
 * 聊天文件实体（MyBatis-Plus）
 * <p>
 * 表名 chat_file。索引由 Flyway 迁移脚本管理。
 * <p>
 * 注意：{@code isReceiverDelete}/{@code isSenderDelete} 是接收方/发送方各自的"是否删除"业务标志，
 *       分别是普通 boolean 字段，**不是**软删除字段；{@code isDeleted} 才是 {@link TableLogic} 软删除字段。
 */
@Data
@TableName("chat_file")
public class ChatFile {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("conversation_id")
    private Long conversationId;

    @TableField("sender_id")
    private Long senderId;

    @TableField("file_path")
    private String filePath;

    @TableField("original_name")
    private String originalName;

    @TableField("file_size")
    private Long fileSize;

    @TableField("mime_type")
    private String mimeType;

    /**
     * 软删除字段：0=未删，1=已删。{@link TableLogic} 自动处理 WHERE 与 delete 翻译。
     */
    @TableField("is_deleted")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted = 0;

    /**
     * 业务字段：接收方是否删除。普通 boolean，**非**软删除字段，不加 @TableLogic。
     */
    @TableField("is_receiver_delete")
    private Boolean isReceiverDelete = false;

    /**
     * 业务字段：发送方是否删除。普通 boolean，**非**软删除字段，不加 @TableLogic。
     */
    @TableField("is_sender_delete")
    private Boolean isSenderDelete = false;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @Version
    @TableField("version")
    private Integer version;
}
