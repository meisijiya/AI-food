package com.ai.food.mapper;

import com.ai.food.model.ChatFile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天文件 Mapper（MyBatis-Plus）。
 * <p>
 * 由原 JPA {@code ChatFileRepository} 翻译而来，对应表 {@code chat_file}。
 * 实体带 {@code @TableLogic(value="0", delval="1")}，但 {@code @TableLogic} 不会自动包裹
 * 显式 {@code @Select} 注解的 SQL，因此本 Mapper 在 SELECT 中显式追加 {@code AND is_deleted = 0}
 * 以保持原 JPA {@code @Where} 软删除过滤语义。
 * </p>
 */
@Mapper
public interface ChatFileMapper extends BaseMapper<ChatFile> {

    /**
     * 列出某会话下全部文件（按时间倒序由 caller 自行排序）
     */
    @Select("SELECT * FROM chat_file WHERE conversation_id = #{conversationId} AND is_deleted = 0")
    List<ChatFile> findByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 列出所有已软删除的文件（清理任务用）
     */
    @Select("SELECT * FROM chat_file WHERE is_deleted = 1")
    List<ChatFile> findAllByIsDeletedTrue();

    /**
     * 列出某截止时间之前创建的文件
     */
    @Select("SELECT * FROM chat_file WHERE created_at < #{cutoff} AND is_deleted = 0")
    List<ChatFile> findByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);

    /**
     * 软删除某会话下的全部文件
     */
    @Update("UPDATE chat_file SET is_deleted = 1, version = version + 1 WHERE conversation_id = #{conversationId}")
    int softDeleteByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 软删除某会话在指定时间之前的文件
     */
    @Update("UPDATE chat_file SET is_deleted = 1, version = version + 1 " +
           "WHERE conversation_id = #{conversationId} AND created_at < #{before}")
    int softDeleteByConversationIdBefore(@Param("conversationId") Long conversationId,
                                          @Param("before") LocalDateTime before);

    /**
     * 硬删除所有已软删除的文件
     */
    @Delete("DELETE FROM chat_file WHERE is_deleted = 1")
    int hardDeleteAllSoftDeleted();

    /**
     * 硬删除某会话下所有已软删除的文件（native query）
     */
    @Delete("DELETE FROM chat_file WHERE conversation_id = #{conversationId} AND is_deleted = 1")
    int hardDeleteByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 软删除所有过期的文件
     */
    @Update("UPDATE chat_file SET is_deleted = 1, version = version + 1 WHERE created_at < #{cutoff}")
    int softDeleteExpired(@Param("cutoff") LocalDateTime cutoff);

    /**
     * 更新文件所属会话 id（迁移到新会话时使用）
     */
    @Update("UPDATE chat_file SET conversation_id = #{conversationId}, version = version + 1 WHERE id = #{id}")
    int updateConversationId(@Param("id") Long id, @Param("conversationId") Long conversationId);

    /**
     * 标记接收方已删除
     */
    @Update("UPDATE chat_file SET is_receiver_delete = 1, version = version + 1 WHERE id = #{fileId}")
    int markReceiverDeleted(@Param("fileId") Long fileId);

    /**
     * 标记发送方已删除
     */
    @Update("UPDATE chat_file SET is_sender_delete = 1, version = version + 1 WHERE id = #{fileId}")
    int markSenderDeleted(@Param("fileId") Long fileId);

    /**
     * 软删除该文件
     */
    @Update("UPDATE chat_file SET is_deleted = 1, version = version + 1 WHERE id = #{fileId}")
    int markSoftDeleted(@Param("fileId") Long fileId);

    /**
     * 查找双方都已删除的文件
     */
    @Select("SELECT * FROM chat_file " +
           "WHERE id = #{fileId} AND is_receiver_delete = 1 AND is_sender_delete = 1 LIMIT 1")
    ChatFile findByIdAndBothDeleted(@Param("fileId") Long fileId);

    /**
     * 硬删除单条文件
     */
    @Delete("DELETE FROM chat_file WHERE id = #{fileId}")
    int hardDeleteById(@Param("fileId") Long fileId);
}
