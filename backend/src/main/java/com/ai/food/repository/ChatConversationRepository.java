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
           "AND NOT (c.user1Id = :userId AND c.clearedByUser1 = true) " +
           "AND NOT (c.user2Id = :userId AND c.clearedByUser2 = true) " +
           "ORDER BY c.lastMessageAt DESC")
    List<ChatConversation> findByUserIdOrderByLastMessageAtDesc(@Param("userId") Long userId);

    @Query("SELECT c FROM ChatConversation c WHERE (c.user1Id = :userId OR c.user2Id = :userId) AND c.lastMessageAt > :since ORDER BY c.lastMessageAt DESC")
    List<ChatConversation> findRecentByUserId(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT c FROM ChatConversation c WHERE " +
           "(c.clearedByUser1 = true AND c.clearedByUser2 = true) " +
           "OR (c.lastMessageAt < :cutoff)")
    List<ChatConversation> findClearedOrExpired(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query("UPDATE ChatConversation c SET c.clearedByUser1 = true WHERE c.id = :id")
    void setClearedByUser1(@Param("id") Long id);

    @Modifying
    @Query("UPDATE ChatConversation c SET c.clearedByUser2 = true WHERE c.id = :id")
    void setClearedByUser2(@Param("id") Long id);

    @Modifying
    @Query("UPDATE ChatConversation c SET c.clearedByUser1 = true, c.clearedByUser2 = true WHERE c.id = :id")
    void setClearedByBoth(@Param("id") Long id);

    List<ChatConversation> findByClearedByUser1FalseAndClearedByUser2False();
}
