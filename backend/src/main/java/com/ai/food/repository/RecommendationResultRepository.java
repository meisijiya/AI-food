package com.ai.food.repository;

import com.ai.food.model.RecommendationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Repository
public interface RecommendationResultRepository extends JpaRepository<RecommendationResult, Long> {
    
    Optional<RecommendationResult> findBySessionId(String sessionId);
    
    @Modifying
    @Transactional
    @Query("UPDATE RecommendationResult r SET r.isDeleted = true WHERE r.sessionId = :sessionId")
    void softDeleteBySessionId(String sessionId);

    @Modifying
    @Transactional
    @Query("DELETE FROM RecommendationResult r WHERE r.isDeleted = true")
    int hardDeleteAllSoftDeleted();
}
