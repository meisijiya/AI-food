package com.ai.food.repository;

import com.ai.food.model.CollectedParam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface CollectedParamRepository extends JpaRepository<CollectedParam, Long> {
    
    List<CollectedParam> findBySessionId(String sessionId);
    
    Optional<CollectedParam> findBySessionIdAndParamName(String sessionId, String paramName);
    
    boolean existsBySessionIdAndParamName(String sessionId, String paramName);
    
    long countBySessionId(String sessionId);
    
    @Modifying
    @Transactional
    @Query("UPDATE CollectedParam c SET c.isDeleted = true WHERE c.sessionId = :sessionId")
    void softDeleteBySessionId(String sessionId);
    
    @Modifying
    @Transactional
    @Query("UPDATE CollectedParam c SET c.isDeleted = true WHERE c.sessionId IN :sessionIds")
    void softDeleteBySessionIdIn(List<String> sessionIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM CollectedParam c WHERE c.isDeleted = true")
    int hardDeleteAllSoftDeleted();
}
