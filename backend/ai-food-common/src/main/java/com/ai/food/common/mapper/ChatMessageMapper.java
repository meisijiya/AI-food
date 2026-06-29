package com.ai.food.common.mapper;

import com.ai.food.common.model.ChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天消息 Mapper（MyBatis-Plus）。
 * <p>
 * 由原 JPA {@code ChatMessageRepository} 翻译而来，对应表 {@code chat_message}。
 * 实体带 {@code @TableLogic(value="0", delval="1")}，但 {@code @TableLogic} 不会自动包裹显式
 * {@code @Select} 注解的 SQL，因此本 Mapper 在 SELECT 中显式追加 {@code AND is_deleted = 0}
 * 以保持原 JPA {@code @Where} 软删除过滤语义。
 * </p>
 *
 * <p>
 * 原 JPA 方法中带 {@code Pageable} 参数的在 MP 中改为 {@code IPage<T>} 参数 +
 * {@code List<T>} 返回（MP 分页拦截器会按 {@code IPage} 自动注入 LIMIT/OFFSET）；
 * {@code Page<T>} 的最终包装由调用方在 service 层完成，超出 Mapper 翻译范围。
 * </p>
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 分页查询某会话的全部消息（按时间倒序）
     */
    @Select("SELECT * FROM chat_message " +
           "WHERE conversation_id = #{conversationId} AND is_deleted = 0 " +
           "ORDER BY created_at DESC")
    List<ChatMessage> findByConversationIdOrderByCreatedAtDesc(@Param("conversationId") Long conversationId,
                                                                IPage<ChatMessage> page);

    /**
     * 查询某会话的全部消息（按时间正序）
     */
    @Select("SELECT * FROM chat_message " +
           "WHERE conversation_id = #{conversationId} AND is_deleted = 0 " +
           "ORDER BY created_at ASC")
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(@Param("conversationId") Long conversationId);

    /**
     * 按 photoId 查找消息（可能为 null，未匹配返回 null）
     */
    @Select("SELECT * FROM chat_message WHERE photo_id = #{photoId} AND is_deleted = 0 LIMIT 1")
    ChatMessage findByPhotoId(@Param("photoId") Long photoId);

    /**
     * 按 fileId 查找消息
     */
    @Select("SELECT * FROM chat_message WHERE file_id = #{fileId} AND is_deleted = 0 LIMIT 1")
    ChatMessage findByFileId(@Param("fileId") Long fileId);

    /**
     * 统计某接收者所有未读消息数
     */
    @Select("SELECT COUNT(*) FROM chat_message WHERE receiver_id = #{userId} AND is_read = 0 AND is_deleted = 0")
    long countUnreadByReceiverId(@Param("userId") Long userId);

    /**
     * 按会话分组统计某接收者的未读消息数
     */
    @Select("SELECT conversation_id, COUNT(*) FROM chat_message " +
           "WHERE receiver_id = #{userId} AND is_read = 0 AND is_deleted = 0 " +
           "GROUP BY conversation_id")
    List<Object[]> countUnreadByConversationId(@Param("userId") Long userId);

    /**
     * 将某会话内某用户未读消息标记为已读
     */
    @Update("UPDATE chat_message SET is_read = 1, version = version + 1 " +
           "WHERE conversation_id = #{conversationId} AND receiver_id = #{userId} AND is_read = 0")
    int markAsRead(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    /**
     * 统计某会话内某接收者的未读消息数
     */
    @Select("SELECT COUNT(*) FROM chat_message " +
           "WHERE conversation_id = #{conversationId} AND receiver_id = #{userId} AND is_read = 0 AND is_deleted = 0")
    long countUnreadByConversationIdAndReceiverId(@Param("conversationId") Long conversationId,
                                                  @Param("userId") Long userId);

    /**
     * 软删除某会话在指定时间之前的所有消息
     */
    @Update("UPDATE chat_message SET is_deleted = 1, version = version + 1 " +
           "WHERE conversation_id = #{conversationId} AND created_at < #{before}")
    int softDeleteByConversationIdBefore(@Param("conversationId") Long conversationId,
                                          @Param("before") LocalDateTime before);

    /**
     * 分页查询某会话在某时间点之后的消息（按时间倒序）
     */
    @Select("SELECT * FROM chat_message " +
           "WHERE conversation_id = #{conversationId} AND created_at > #{after} AND is_deleted = 0 " +
           "ORDER BY created_at DESC")
    List<ChatMessage> findByConversationIdAfterOrderByCreatedAtDesc(@Param("conversationId") Long conversationId,
                                                                     @Param("after") LocalDateTime after,
                                                                     IPage<ChatMessage> page);

    /**
     * 查询某会话最近的消息（按时间倒序，配合 IPage 实现 LIMIT）
     */
    @Select("SELECT * FROM chat_message " +
           "WHERE conversation_id = #{conversationId} AND is_deleted = 0 " +
           "ORDER BY created_at DESC")
    List<ChatMessage> findLastMessageByConversationId(@Param("conversationId") Long conversationId,
                                                       IPage<ChatMessage> page);

    /**
     * 硬删除某会话下所有已软删除的消息（native query，绕过 @TableLogic 过滤）
     */
    @Delete("DELETE FROM chat_message WHERE conversation_id = #{conversationId} AND is_deleted = 1")
    int hardDeleteByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 硬删除所有在指定时间之前已软删除的消息（native query）
     */
    @Delete("DELETE FROM chat_message WHERE is_deleted = 1 AND created_at < #{before}")
    int hardDeleteOldSoftDeleted(@Param("before") LocalDateTime before);

    /**
     * 按 id 软删除单条消息
     */
    @Update("UPDATE chat_message SET is_deleted = 1, version = version + 1 WHERE id = #{messageId}")
    int softDeleteById(@Param("messageId") Long messageId);
}
