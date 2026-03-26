package com.ai.food.repository;

import com.ai.food.model.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

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
    @Query("UPDATE ChatMessage m SET m.isDeleted = true WHERE m.conversationId = :conversationId")
    void softDeleteByConversationId(@Param("conversationId") Long conversationId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.isDeleted = true WHERE m.conversationId = :conversationId AND m.createdAt < :before")
    void softDeleteByConversationIdBefore(@Param("conversationId") Long conversationId, @Param("before") java.time.LocalDateTime before);

    @Query("SELECT m FROM ChatMessage m WHERE m.conversationId = :conversationId AND m.isDeleted = false AND m.createdAt > :after ORDER BY m.createdAt DESC")
    Page<ChatMessage> findByConversationIdAfterOrderByCreatedAtDesc(@Param("conversationId") Long conversationId, @Param("after") java.time.LocalDateTime after, Pageable pageable);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.conversationId = :conversationId AND m.receiverId = :userId AND m.isRead = false AND m.isDeleted = false AND m.createdAt > :after")
    long countUnreadAfter(@Param("conversationId") Long conversationId, @Param("userId") Long userId, @Param("after") java.time.LocalDateTime after);

    @Query("SELECT m FROM ChatMessage m WHERE m.conversationId = :conversationId AND m.isDeleted = false ORDER BY m.createdAt DESC")
    List<ChatMessage> findLastMessageByConversationId(@Param("conversationId") Long conversationId, Pageable pageable);
}
