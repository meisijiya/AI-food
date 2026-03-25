package com.ai.food.repository;

import com.ai.food.model.ChatPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatPhotoRepository extends JpaRepository<ChatPhoto, Long> {

    List<ChatPhoto> findByConversationId(Long conversationId);

    List<ChatPhoto> findAllByIsDeletedTrue();

    List<ChatPhoto> findByCreatedAtBefore(LocalDateTime cutoff);

    @Modifying
    @Query("UPDATE ChatPhoto p SET p.isDeleted = true WHERE p.conversationId = :conversationId")
    void softDeleteByConversationId(@Param("conversationId") Long conversationId);

    @Modifying
    @Query("DELETE FROM ChatPhoto p WHERE p.isDeleted = true")
    int hardDeleteAllSoftDeleted();

    @Modifying
    @Query("UPDATE ChatPhoto p SET p.isDeleted = true WHERE p.createdAt < :cutoff")
    void softDeleteExpired(@Param("cutoff") LocalDateTime cutoff);
}
