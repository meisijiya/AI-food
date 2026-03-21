package com.ai.food.repository;

import com.ai.food.model.CollectedParam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CollectedParamRepository extends JpaRepository<CollectedParam, Long> {
    
    List<CollectedParam> findBySessionId(String sessionId);
    
    Optional<CollectedParam> findBySessionIdAndParamName(String sessionId, String paramName);
    
    boolean existsBySessionIdAndParamName(String sessionId, String paramName);
    
    long countBySessionId(String sessionId);
}