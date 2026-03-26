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

    @Query("SELECT c FROM ChatConversation c WHERE (c.user1Id = :userId OR c.user2Id = :userId) " +
           "ORDER BY c.lastMessageAt DESC")
    List<ChatConversation> findByUserIdOrderByLastMessageAtDesc(@Param("userId") Long userId);

    @Query("SELECT c FROM ChatConversation c WHERE (c.user1Id = :userId OR c.user2Id = :userId) AND c.lastMessageAt > :since ORDER BY c.lastMessageAt DESC")
    List<ChatConversation> findRecentByUserId(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Modifying
    @Query("UPDATE ChatConversation c SET c.clearedAtUser1 = :clearedAt WHERE c.id = :id")
    void setClearedAtUser1(@Param("id") Long id, @Param("clearedAt") LocalDateTime clearedAt);

    @Modifying
    @Query("UPDATE ChatConversation c SET c.clearedAtUser2 = :clearedAt WHERE c.id = :id")
    void setClearedAtUser2(@Param("id") Long id, @Param("clearedAt") LocalDateTime clearedAt);

    @Modifying
    @Query("UPDATE ChatConversation c SET c.clearedAtUser1 = null WHERE c.id = :id")
    void resetClearedAtUser1(@Param("id") Long id);

    @Modifying
    @Query("UPDATE ChatConversation c SET c.clearedAtUser2 = null WHERE c.id = :id")
    void resetClearedAtUser2(@Param("id") Long id);

    @Modifying
    @Query("UPDATE ChatConversation c SET c.lastMessage = :lastMessage, c.lastMessageAt = :lastMessageAt WHERE c.id = :id")
    void updateLastMessage(@Param("id") Long id, @Param("lastMessage") String lastMessage, @Param("lastMessageAt") LocalDateTime lastMessageAt);
}
