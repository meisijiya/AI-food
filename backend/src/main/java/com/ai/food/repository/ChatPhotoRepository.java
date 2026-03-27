package com.ai.food.repository;

import com.ai.food.model.ChatPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatPhotoRepository extends JpaRepository<ChatPhoto, Long> {

    List<ChatPhoto> findByConversationId(Long conversationId);

    Optional<ChatPhoto> findByOriginalPath(String originalPath);

    Optional<ChatPhoto> findByThumbnailPath(String thumbnailPath);

    List<ChatPhoto> findAllByIsDeletedTrue();

    List<ChatPhoto> findByCreatedAtBefore(LocalDateTime cutoff);

    @Modifying
    @Query("UPDATE ChatPhoto p SET p.isDeleted = true WHERE p.conversationId = :conversationId")
    void softDeleteByConversationId(@Param("conversationId") Long conversationId);

    @Modifying
    @Query("UPDATE ChatPhoto p SET p.isDeleted = true WHERE p.conversationId = :conversationId AND p.createdAt < :before")
    void softDeleteByConversationIdBefore(@Param("conversationId") Long conversationId, @Param("before") LocalDateTime before);

    @Modifying
    @Query("DELETE FROM ChatPhoto p WHERE p.isDeleted = true")
    int hardDeleteAllSoftDeleted();

    @Modifying
    @Query(value = "DELETE FROM chat_photo WHERE conversation_id = :conversationId AND is_deleted = true", nativeQuery = true)
    int hardDeleteByConversationId(@Param("conversationId") Long conversationId);

    @Modifying
    @Query("UPDATE ChatPhoto p SET p.isDeleted = true WHERE p.createdAt < :cutoff")
    void softDeleteExpired(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query("UPDATE ChatPhoto p SET p.conversationId = :conversationId WHERE p.id = :id")
    void updateConversationId(@Param("id") Long id, @Param("conversationId") Long conversationId);

    @Modifying
    @Query("UPDATE ChatPhoto p SET p.isReceiverDelete = true WHERE p.id = :photoId")
    void markReceiverDeleted(@Param("photoId") Long photoId);

    @Modifying
    @Query("UPDATE ChatPhoto p SET p.isSenderDelete = true WHERE p.id = :photoId")
    void markSenderDeleted(@Param("photoId") Long photoId);

    @Modifying
    @Query("UPDATE ChatPhoto p SET p.isDeleted = true WHERE p.id = :photoId")
    void markSoftDeleted(@Param("photoId") Long photoId);

    @Query("SELECT p FROM ChatPhoto p WHERE p.id = :photoId AND p.isReceiverDelete = true AND p.isSenderDelete = true")
    Optional<ChatPhoto> findByIdAndBothDeleted(@Param("photoId") Long photoId);

    @Modifying
    @Query("DELETE FROM ChatPhoto p WHERE p.id = :photoId")
    void hardDeleteById(@Param("photoId") Long photoId);
}
