package com.ai.food.service.record;

import com.ai.food.model.CollectedParam;
import com.ai.food.model.ConversationSession;
import com.ai.food.model.Photo;
import com.ai.food.model.QaRecord;
import com.ai.food.model.RecommendationResult;
import com.ai.food.repository.CollectedParamRepository;
import com.ai.food.repository.ConversationSessionRepository;
import com.ai.food.repository.PhotoRepository;
import com.ai.food.repository.QaRecordRepository;
import com.ai.food.repository.RecommendationResultRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ConversationSessionRepository conversationSessionRepository;
    private final RecommendationResultRepository recommendationResultRepository;
    private final CollectedParamRepository collectedParamRepository;
    private final QaRecordRepository qaRecordRepository;
    private final PhotoRepository photoRepository;
    private final StringRedisTemplate redisTemplate;

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
                item.setPhotoUrl(r.getPhotoUrl());
                item.setComment(r.getComment());
                item.setSimilarityScore(r.getSimilarityScore());
            });

            photoRepository.findFirstByRelatedSessionIdOrderByCreatedAtDesc(session.getSessionId())
                    .ifPresent(p -> item.setThumbnailUrl(p.getThumbnailPath()));

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
        // 删除硬盘上的照片文件
        photoRepository.findFirstByRelatedSessionIdOrderByCreatedAtDesc(sessionId).ifPresent(photo -> {
            deletePhysicalFile(photo.getOriginalPath());
            deletePhysicalFile(photo.getThumbnailPath());
            photo.setIsDeleted(true);
            photoRepository.save(photo);
        });
        qaRecordRepository.softDeleteBySessionId(sessionId);
        collectedParamRepository.softDeleteBySessionId(sessionId);
        recommendationResultRepository.softDeleteBySessionId(sessionId);
        conversationSessionRepository.softDeleteBySessionId(sessionId);
    }

    @Transactional
    public void batchDeleteRecords(List<String> sessionIds) {
        log.info("Batch soft deleting {} records", sessionIds.size());
        for (String sessionId : sessionIds) {
            photoRepository.findFirstByRelatedSessionIdOrderByCreatedAtDesc(sessionId).ifPresent(photo -> {
                deletePhysicalFile(photo.getOriginalPath());
                deletePhysicalFile(photo.getThumbnailPath());
                photo.setIsDeleted(true);
                photoRepository.save(photo);
            });
            qaRecordRepository.softDeleteBySessionId(sessionId);
            collectedParamRepository.softDeleteBySessionId(sessionId);
            recommendationResultRepository.softDeleteBySessionId(sessionId);
            conversationSessionRepository.softDeleteBySessionId(sessionId);
        }
    }

    @Transactional
    public void updateRecommendationPhoto(String sessionId, String photoUrl) {
        log.info("Updating photo for session: {}", sessionId);
        recommendationResultRepository.findBySessionId(sessionId).ifPresent(r -> {
            r.setPhotoUrl(photoUrl);
            recommendationResultRepository.save(r);
        });
        conversationSessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            Long userId = session.getUserId();
            if (userId != null) {
                redisTemplate.delete("pending:recommend:" + userId);
            }
        });
    }

    @Transactional
    public void deleteRecommendationPhoto(String sessionId) {
        log.info("Deleting photo for session: {}", sessionId);
        // 清除 recommendation_result 的 photo_url
        recommendationResultRepository.findBySessionId(sessionId).ifPresent(r -> {
            r.setPhotoUrl(null);
            recommendationResultRepository.save(r);
        });
        // 软删除 photo 表记录 + 删除硬盘文件
        photoRepository.findFirstByRelatedSessionIdOrderByCreatedAtDesc(sessionId).ifPresent(photo -> {
            deletePhysicalFile(photo.getOriginalPath());
            deletePhysicalFile(photo.getThumbnailPath());
            photo.setIsDeleted(true);
            photoRepository.save(photo);
        });
    }

    @Transactional
    public void updateComment(String sessionId, String comment) {
        log.info("Updating comment for session: {}", sessionId);
        recommendationResultRepository.findBySessionId(sessionId).ifPresent(r -> {
            r.setComment(comment);
            recommendationResultRepository.save(r);
        });
    }

    private void deletePhysicalFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;
        try {
            java.nio.file.Path root = java.nio.file.Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath();
            // relativePath 如 /uploads/photos/20260322/xxx.jpg，去掉 /uploads 前缀
            String relative = relativePath.startsWith("/uploads") ? relativePath.substring("/uploads".length()) : relativePath;
            java.nio.file.Path filePath = root.resolve(relative.startsWith("/") ? relative.substring(1) : relative);
            if (java.nio.file.Files.deleteIfExists(filePath)) {
                log.info("Deleted physical file: {}", filePath);
            }
        } catch (Exception e) {
            log.warn("Failed to delete physical file: {}", relativePath, e);
        }
    }

    public RecordDetail getRecordDetail(String sessionId) {
        ConversationSession session = conversationSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("会话不存在"));

        Optional<RecommendationResult> optResult =
                recommendationResultRepository.findBySessionId(sessionId);
        List<CollectedParam> params = collectedParamRepository.findBySessionId(sessionId);
        List<QaRecord> qaRecords = qaRecordRepository.findBySessionIdOrderByQuestionOrderAsc(sessionId);
        Optional<Photo> photo = photoRepository.findFirstByRelatedSessionIdOrderByCreatedAtDesc(sessionId);

        RecordDetail detail = new RecordDetail();
        detail.setSession(session);
        detail.setRecommendation(optResult.orElse(null));
        detail.setCollectedParams(params);
        detail.setQaRecords(qaRecords);
        detail.setPhoto(photo.orElse(null));
        return detail;
    }

    public Map<String, Object> getPendingRecommendation(Long userId) {
        String cacheKey = "pending:recommend:" + userId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached == null || cached.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(cached, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse cached pending recommendation for user {}", userId, e);
            return null;
        }
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
        private String photoUrl;
        private String comment;
        private Number similarityScore;
        private String thumbnailUrl;
    }

    @Data
    public static class RecordDetail {
        private ConversationSession session;
        private RecommendationResult recommendation;
        private List<CollectedParam> collectedParams;
        private List<QaRecord> qaRecords;
        private Photo photo;
    }
}
