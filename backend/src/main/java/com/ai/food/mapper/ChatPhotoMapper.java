package com.ai.food.mapper;

import com.ai.food.model.ChatPhoto;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天图片 Mapper（MyBatis-Plus）。
 * <p>
 * 由原 JPA {@code ChatPhotoRepository} 翻译而来，对应表 {@code chat_photo}。
 * 实体带 {@code @TableLogic(value="0", delval="1")}，但 {@code @TableLogic} 不会自动包裹
 * 显式 {@code @Select} 注解的 SQL，因此本 Mapper 在 SELECT 中显式追加 {@code AND is_deleted = 0}
 * 以保持原 JPA {@code @Where} 软删除过滤语义。
 * </p>
 */
@Mapper
public interface ChatPhotoMapper extends BaseMapper<ChatPhoto> {

    /**
     * 列出某会话下全部图片
     */
    @Select("SELECT * FROM chat_photo WHERE conversation_id = #{conversationId} AND is_deleted = 0")
    List<ChatPhoto> findByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 按原图路径查找图片
     */
    @Select("SELECT * FROM chat_photo WHERE original_path = #{originalPath} AND is_deleted = 0 LIMIT 1")
    ChatPhoto findByOriginalPath(@Param("originalPath") String originalPath);

    /**
     * 按缩略图路径查找图片
     */
    @Select("SELECT * FROM chat_photo WHERE thumbnail_path = #{thumbnailPath} AND is_deleted = 0 LIMIT 1")
    ChatPhoto findByThumbnailPath(@Param("thumbnailPath") String thumbnailPath);

    /**
     * 列出所有已软删除的图片（清理任务用）
     */
    @Select("SELECT * FROM chat_photo WHERE is_deleted = 1")
    List<ChatPhoto> findAllByIsDeletedTrue();

    /**
     * 列出某截止时间之前创建的图片
     */
    @Select("SELECT * FROM chat_photo WHERE created_at < #{cutoff} AND is_deleted = 0")
    List<ChatPhoto> findByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);

    /**
     * 软删除某会话下的全部图片
     */
    @Update("UPDATE chat_photo SET is_deleted = 1, version = version + 1 WHERE conversation_id = #{conversationId}")
    int softDeleteByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 软删除某会话在指定时间之前的图片
     */
    @Update("UPDATE chat_photo SET is_deleted = 1, version = version + 1 " +
           "WHERE conversation_id = #{conversationId} AND created_at < #{before}")
    int softDeleteByConversationIdBefore(@Param("conversationId") Long conversationId,
                                          @Param("before") LocalDateTime before);

    /**
     * 硬删除所有已软删除的图片
     */
    @Delete("DELETE FROM chat_photo WHERE is_deleted = 1")
    int hardDeleteAllSoftDeleted();

    /**
     * 硬删除某会话下所有已软删除的图片（native query）
     */
    @Delete("DELETE FROM chat_photo WHERE conversation_id = #{conversationId} AND is_deleted = 1")
    int hardDeleteByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 软删除所有过期的图片
     */
    @Update("UPDATE chat_photo SET is_deleted = 1, version = version + 1 WHERE created_at < #{cutoff}")
    int softDeleteExpired(@Param("cutoff") LocalDateTime cutoff);

    /**
     * 更新图片所属会话 id
     */
    @Update("UPDATE chat_photo SET conversation_id = #{conversationId}, version = version + 1 WHERE id = #{id}")
    int updateConversationId(@Param("id") Long id, @Param("conversationId") Long conversationId);

    /**
     * 标记接收方已删除
     */
    @Update("UPDATE chat_photo SET is_receiver_delete = 1, version = version + 1 WHERE id = #{photoId}")
    int markReceiverDeleted(@Param("photoId") Long photoId);

    /**
     * 标记发送方已删除
     */
    @Update("UPDATE chat_photo SET is_sender_delete = 1, version = version + 1 WHERE id = #{photoId}")
    int markSenderDeleted(@Param("photoId") Long photoId);

    /**
     * 软删除该图片
     */
    @Update("UPDATE chat_photo SET is_deleted = 1, version = version + 1 WHERE id = #{photoId}")
    int markSoftDeleted(@Param("photoId") Long photoId);

    /**
     * 查找双方都已删除的图片
     */
    @Select("SELECT * FROM chat_photo " +
           "WHERE id = #{photoId} AND is_receiver_delete = 1 AND is_sender_delete = 1 LIMIT 1")
    ChatPhoto findByIdAndBothDeleted(@Param("photoId") Long photoId);

    /**
     * 硬删除单条图片
     */
    @Delete("DELETE FROM chat_photo WHERE id = #{photoId}")
    int hardDeleteById(@Param("photoId") Long photoId);
}
