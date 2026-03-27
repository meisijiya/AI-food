package com.ai.food.repository;

import com.ai.food.model.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    Optional<ChatMessage> findByPhotoId(Long photoId);

    Optional<ChatMessage> findByFileId(Long fileId);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.receiverId = :userId AND m.isRead = false")
    long countUnreadByReceiverId(@Param("userId") Long userId);

    @Query("SELECT m.conversationId, COUNT(m) FROM ChatMessage m WHERE m.receiverId = :userId AND m.isRead = false GROUP BY m.conversationId")
    List<Object[]> countUnreadByConversationId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.conversationId = :conversationId AND m.receiverId = :userId AND m.isRead = false")
    int markAsRead(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.conversationId = :conversationId AND m.receiverId = :userId AND m.isRead = false")
    long countUnreadByConversationIdAndReceiverId(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.isDeleted = true WHERE m.conversationId = :conversationId AND m.createdAt < :before")
    void softDeleteByConversationIdBefore(@Param("conversationId") Long conversationId, @Param("before") LocalDateTime before);

    @Query("SELECT m FROM ChatMessage m WHERE m.conversationId = :conversationId AND m.createdAt > :after ORDER BY m.createdAt DESC")
    Page<ChatMessage> findByConversationIdAfterOrderByCreatedAtDesc(@Param("conversationId") Long conversationId, @Param("after") LocalDateTime after, Pageable pageable);

    @Query("SELECT m FROM ChatMessage m WHERE m.conversationId = :conversationId ORDER BY m.createdAt DESC")
    List<ChatMessage> findLastMessageByConversationId(@Param("conversationId") Long conversationId, Pageable pageable);

    /**
     * 硬删除指定对话中所有已软删除的消息（@Where 注解会自动排除 isDeleted=true，
     * 所以此处需要使用 native query 或在 @Where 之外操作）
     */
    @Modifying
    @Query(value = "DELETE FROM chat_message WHERE conversation_id = :conversationId AND is_deleted = true", nativeQuery = true)
    int hardDeleteByConversationId(@Param("conversationId") Long conversationId);

    @Modifying
    @Query(value = "DELETE FROM chat_message WHERE is_deleted = true AND created_at < :before", nativeQuery = true)
    int hardDeleteOldSoftDeleted(@Param("before") LocalDateTime before);
}
