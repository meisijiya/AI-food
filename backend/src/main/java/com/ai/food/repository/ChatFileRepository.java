package com.ai.food.repository;

import com.ai.food.model.ChatFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatFileRepository extends JpaRepository<ChatFile, Long> {

    List<ChatFile> findByConversationId(Long conversationId);

    List<ChatFile> findAllByIsDeletedTrue();

    List<ChatFile> findByCreatedAtBefore(LocalDateTime cutoff);

    @Modifying
    @Query("UPDATE ChatFile f SET f.isDeleted = true WHERE f.conversationId = :conversationId")
    void softDeleteByConversationId(@Param("conversationId") Long conversationId);

    @Modifying
    @Query("DELETE FROM ChatFile f WHERE f.isDeleted = true")
    int hardDeleteAllSoftDeleted();

    @Modifying
    @Query("UPDATE ChatFile f SET f.isDeleted = true WHERE f.createdAt < :cutoff")
    void softDeleteExpired(@Param("cutoff") LocalDateTime cutoff);
}
