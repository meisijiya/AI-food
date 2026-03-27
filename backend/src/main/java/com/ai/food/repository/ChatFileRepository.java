package com.ai.food.repository;

import com.ai.food.model.ChatFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatFileRepository extends JpaRepository<ChatFile, Long> {

    List<ChatFile> findByConversationId(Long conversationId);

    List<ChatFile> findAllByIsDeletedTrue();

    List<ChatFile> findByCreatedAtBefore(LocalDateTime cutoff);

    @Modifying
    @Query("UPDATE ChatFile f SET f.isDeleted = true WHERE f.conversationId = :conversationId")
    void softDeleteByConversationId(@Param("conversationId") Long conversationId);

    @Modifying
    @Query("UPDATE ChatFile f SET f.isDeleted = true WHERE f.conversationId = :conversationId AND f.createdAt < :before")
    void softDeleteByConversationIdBefore(@Param("conversationId") Long conversationId, @Param("before") LocalDateTime before);

    @Modifying
    @Query("DELETE FROM ChatFile f WHERE f.isDeleted = true")
    int hardDeleteAllSoftDeleted();

    @Modifying
    @Query(value = "DELETE FROM chat_file WHERE conversation_id = :conversationId AND is_deleted = true", nativeQuery = true)
    int hardDeleteByConversationId(@Param("conversationId") Long conversationId);

    @Modifying
    @Query("UPDATE ChatFile f SET f.isDeleted = true WHERE f.createdAt < :cutoff")
    void softDeleteExpired(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query("UPDATE ChatFile f SET f.conversationId = :conversationId WHERE f.id = :id")
    void updateConversationId(@Param("id") Long id, @Param("conversationId") Long conversationId);

    @Modifying
    @Query("UPDATE ChatFile f SET f.isReceiverDelete = true WHERE f.id = :fileId")
    void markReceiverDeleted(@Param("fileId") Long fileId);

    @Modifying
    @Query("UPDATE ChatFile f SET f.isSenderDelete = true WHERE f.id = :fileId")
    void markSenderDeleted(@Param("fileId") Long fileId);

    @Query("SELECT f FROM ChatFile f WHERE f.id = :fileId AND f.isReceiverDelete = true AND f.isSenderDelete = true")
    Optional<ChatFile> findByIdAndBothDeleted(@Param("fileId") Long fileId);

    @Modifying
    @Query("DELETE FROM ChatFile f WHERE f.id = :fileId")
    void hardDeleteById(@Param("fileId") Long fileId);
}
