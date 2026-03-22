package com.ai.food.repository;

import com.ai.food.model.ConversationSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ConversationSessionRepository extends JpaRepository<ConversationSession, Long> {

    Optional<ConversationSession> findBySessionId(String sessionId);

    boolean existsBySessionId(String sessionId);

    Page<ConversationSession> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}