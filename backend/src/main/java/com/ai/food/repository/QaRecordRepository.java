package com.ai.food.repository;

import com.ai.food.model.QaRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QaRecordRepository extends JpaRepository<QaRecord, Long> {
    
    List<QaRecord> findBySessionIdOrderByQuestionOrderAsc(String sessionId);
    
    List<QaRecord> findBySessionId(String sessionId);
    
    long countBySessionId(String sessionId);
}