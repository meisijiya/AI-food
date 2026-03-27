package com.ai.food.repository;

import com.ai.food.model.ConversationSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationSessionRepository extends JpaRepository<ConversationSession, Long> {

    Optional<ConversationSession> findBySessionId(String sessionId);

    boolean existsBySessionId(String sessionId);

    Page<ConversationSession> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<ConversationSession> findByUserIdOrderByCreatedAtAsc(Long userId, Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE ConversationSession c SET c.isDeleted = true, c.deletedAt = CURRENT_TIMESTAMP WHERE c.sessionId = :sessionId")
    void softDeleteBySessionId(String sessionId);
    
    @Modifying
    @Transactional
    @Query("UPDATE ConversationSession c SET c.isDeleted = true, c.deletedAt = CURRENT_TIMESTAMP WHERE c.sessionId IN :sessionIds")
    void softDeleteBySessionIdIn(List<String> sessionIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM ConversationSession c WHERE c.isDeleted = true")
    int hardDeleteAllSoftDeleted();
}
