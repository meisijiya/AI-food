package com.ai.food.repository;

import com.ai.food.model.ShareRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ShareRecordRepository extends JpaRepository<ShareRecord, Long> {

    Optional<ShareRecord> findByShareToken(String shareToken);

    Optional<ShareRecord> findBySessionIdAndUserId(String sessionId, Long userId);
}
