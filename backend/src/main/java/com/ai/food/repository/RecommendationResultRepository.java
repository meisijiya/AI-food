package com.ai.food.repository;

import com.ai.food.model.RecommendationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RecommendationResultRepository extends JpaRepository<RecommendationResult, Long> {
    
    Optional<RecommendationResult> findBySessionId(String sessionId);
}