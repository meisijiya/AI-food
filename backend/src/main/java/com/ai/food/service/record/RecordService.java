package com.ai.food.service.record;

import com.ai.food.model.CollectedParam;
import com.ai.food.model.ConversationSession;
import com.ai.food.model.QaRecord;
import com.ai.food.model.RecommendationResult;
import com.ai.food.repository.CollectedParamRepository;
import com.ai.food.repository.ConversationSessionRepository;
import com.ai.food.repository.QaRecordRepository;
import com.ai.food.repository.RecommendationResultRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordService {

    private final ConversationSessionRepository conversationSessionRepository;
    private final RecommendationResultRepository recommendationResultRepository;
    private final CollectedParamRepository collectedParamRepository;
    private final QaRecordRepository qaRecordRepository;

    public Page<RecordListItem> getRecordList(Long userId, int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ConversationSession> sessions;
        if ("asc".equalsIgnoreCase(sort)) {
            sessions = conversationSessionRepository.findByUserIdOrderByCreatedAtAsc(userId, pageable);
        } else {
            sessions = conversationSessionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        return sessions.map(session -> {
            RecordListItem item = new RecordListItem();
            item.setSessionId(session.getSessionId());
            item.setMode(session.getMode());
            item.setStatus(session.getStatus());
            item.setTotalQuestions(session.getTotalQuestions());
            item.setCurrentQuestionCount(session.getCurrentQuestionCount());
            item.setCreatedAt(session.getCreatedAt());
            item.setCompletedAt(session.getCompletedAt());

            Optional<RecommendationResult> optResult =
                    recommendationResultRepository.findBySessionId(session.getSessionId());
            optResult.ifPresent(r -> {
                item.setFoodName(r.getFoodName());
                item.setReason(r.getReason());
                item.setSimilarityScore(r.getSimilarityScore());
            });

            if (item.getFoodName() == null || item.getFoodName().isBlank()) {
                item.setFoodName("暂无推荐结果");
            }
            if (item.getReason() == null || item.getReason().isBlank()) {
                item.setReason("该会话暂无可展示的推荐说明");
            }

            return item;
        });
    }

    @Transactional
    public void deleteRecord(String sessionId) {
        log.info("Soft deleting record: {}", sessionId);
        qaRecordRepository.softDeleteBySessionId(sessionId);
        collectedParamRepository.softDeleteBySessionId(sessionId);
        recommendationResultRepository.softDeleteBySessionId(sessionId);
        conversationSessionRepository.softDeleteBySessionId(sessionId);
    }

    @Transactional
    public void batchDeleteRecords(List<String> sessionIds) {
        log.info("Batch soft deleting {} records", sessionIds.size());
        for (String sessionId : sessionIds) {
            qaRecordRepository.softDeleteBySessionId(sessionId);
            collectedParamRepository.softDeleteBySessionId(sessionId);
            recommendationResultRepository.softDeleteBySessionId(sessionId);
            conversationSessionRepository.softDeleteBySessionId(sessionId);
        }
    }

    public RecordDetail getRecordDetail(String sessionId) {
        ConversationSession session = conversationSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("会话不存在"));

        Optional<RecommendationResult> optResult =
                recommendationResultRepository.findBySessionId(sessionId);
        List<CollectedParam> params = collectedParamRepository.findBySessionId(sessionId);
        List<QaRecord> qaRecords = qaRecordRepository.findBySessionIdOrderByQuestionOrderAsc(sessionId);

        RecordDetail detail = new RecordDetail();
        detail.setSession(session);
        detail.setRecommendation(optResult.orElse(null));
        detail.setCollectedParams(params);
        detail.setQaRecords(qaRecords);
        return detail;
    }

    @Data
    public static class RecordListItem {
        private String sessionId;
        private String mode;
        private String status;
        private Integer totalQuestions;
        private Integer currentQuestionCount;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
        private String foodName;
        private String reason;
        private Number similarityScore;
    }

    @Data
    public static class RecordDetail {
        private ConversationSession session;
        private RecommendationResult recommendation;
        private List<CollectedParam> collectedParams;
        private List<QaRecord> qaRecords;
    }
}
