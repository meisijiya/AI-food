package com.ai.food.repository;

import com.ai.food.model.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    Optional<ChatConversation> findByConversationKey(String conversationKey);

    /**
     * 获取用户的聊天列表：
     * - 排除 hiddenAt 在 lastMessageAt 之后的会话（用户已隐藏且无新消息）
     * - 如果有新消息（lastMessageAt > hiddenAt），会话仍会显示
     */
    @Query("SELECT c FROM ChatConversation c WHERE (c.user1Id = :userId OR c.user2Id = :userId) " +
           "AND (:userId = c.user1Id AND (c.hiddenAtUser1 IS NULL OR c.lastMessageAt > c.hiddenAtUser1) " +
           "  OR :userId = c.user2Id AND (c.hiddenAtUser2 IS NULL OR c.lastMessageAt > c.hiddenAtUser2)) " +
           "ORDER BY c.lastMessageAt DESC")
    List<ChatConversation> findByUserIdOrderByLastMessageAtDesc(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE ChatConversation c SET c.clearedAtUser1 = :clearedAt, c.hiddenAtUser1 = :clearedAt WHERE c.id = :id")
    void setClearedAndHiddenAtUser1(@Param("id") Long id, @Param("clearedAt") LocalDateTime clearedAt);

    @Modifying
    @Query("UPDATE ChatConversation c SET c.clearedAtUser2 = :clearedAt, c.hiddenAtUser2 = :clearedAt WHERE c.id = :id")
    void setClearedAndHiddenAtUser2(@Param("id") Long id, @Param("clearedAt") LocalDateTime clearedAt);

    /**
     * 重置 hiddenAt — 新消息到来时，会话重新出现在列表
     * clearedAt 保持不变，确保旧消息继续被过滤
     */
    @Modifying
    @Query("UPDATE ChatConversation c SET c.hiddenAtUser1 = null WHERE c.id = :id")
    void resetHiddenAtUser1(@Param("id") Long id);

    @Modifying
    @Query("UPDATE ChatConversation c SET c.hiddenAtUser2 = null WHERE c.id = :id")
    void resetHiddenAtUser2(@Param("id") Long id);

    @Modifying
    @Query("UPDATE ChatConversation c SET c.lastMessage = :lastMessage, c.lastMessageAt = :lastMessageAt WHERE c.id = :id")
    void updateLastMessage(@Param("id") Long id, @Param("lastMessage") String lastMessage, @Param("lastMessageAt") LocalDateTime lastMessageAt);

    /**
     * 查找双方都已清除的对话（可用于硬删除清理）
     */
    @Query("SELECT c FROM ChatConversation c WHERE c.clearedAtUser1 IS NOT NULL AND c.clearedAtUser2 IS NOT NULL")
    List<ChatConversation> findAllBothCleared();
}
