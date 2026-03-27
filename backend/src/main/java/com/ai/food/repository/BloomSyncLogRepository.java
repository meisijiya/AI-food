package com.ai.food.repository;

import com.ai.food.model.BloomSyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BloomSyncLogRepository extends JpaRepository<BloomSyncLog, Long> {
    
    List<BloomSyncLog> findByUserId(Long userId);
    
    List<BloomSyncLog> findBySyncedAtAfter(LocalDateTime after);
    
    long countByStatus(String status);
}