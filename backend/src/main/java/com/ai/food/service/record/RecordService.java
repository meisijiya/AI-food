package com.ai.food.service.record;

import com.ai.food.common.mapper.CollectedParamMapper;
import com.ai.food.common.mapper.ConversationSessionMapper;
import com.ai.food.common.mapper.FeedCommentMapper;
import com.ai.food.common.mapper.FeedPostMapper;
import com.ai.food.common.mapper.PhotoMapper;
import com.ai.food.common.mapper.QaRecordMapper;
import com.ai.food.common.mapper.RecommendationResultMapper;
import com.ai.food.common.model.CollectedParam;
import com.ai.food.common.model.ConversationSession;
import com.ai.food.common.model.FeedPost;
import com.ai.food.common.model.Photo;
import com.ai.food.common.model.QaRecord;
import com.ai.food.common.model.RecommendationResult;
import com.ai.food.service.bloom.BloomFilterService;
import com.ai.food.service.feed.FeedService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 历史记录业务服务（MyBatis-Plus 迁移版）。
 * <p>
 * 继承 {@link ServiceImpl} 后，{@code baseMapper} 指向 {@link ConversationSessionMapper}；
 * 其余实体走注入的 Mapper 字段。
 * </p>
 * <p>
 * {@link #getRecordList} 保持返回 {@link Page}{@code <RecordListItem>}（spring-data 域）以兼容
 * 上层 Controller；内部用 MP 的 {@link IPage} 查询，最终用 {@link PageImpl} 包装。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecordService extends ServiceImpl<ConversationSessionMapper, ConversationSession> {

    private final RecommendationResultMapper recommendationResultMapper;
    private final CollectedParamMapper collectedParamMapper;
    private final QaRecordMapper qaRecordMapper;
    private final PhotoMapper photoMapper;
    private final FeedPostMapper feedPostMapper;
    private final FeedCommentMapper feedCommentMapper;
    private final FeedService feedService;
    private final StringRedisTemplate redisTemplate;
    private final BloomFilterService bloomFilterService;

    /**
     * 分页查询某用户的对话会话记录（含推荐结果、照片缩略图等聚合信息）。
     * <p>
     * 控制器传 0-based page/size；内部用 MP 的 1-based Page 查询。
     * </p>
     */
    public Page<RecordListItem> getRecordList(Long userId, int page, int size, String sort) {
        IPage<ConversationSession> sessions = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page + 1, size);
        if ("asc".equalsIgnoreCase(sort)) {
            sessions = baseMapper.selectByUserIdOrderByCreatedAtAsc(sessions, userId);
        } else {
            sessions = baseMapper.selectByUserIdOrderByCreatedAtDesc(sessions, userId);
        }

        // Collect all sessionIds for batch fetching
        List<String> sessionIds = new ArrayList<>();
        for (ConversationSession s : sessions.getRecords()) {
            sessionIds.add(s.getSessionId());
        }

        // Batch fetch recommendation results
        Map<String, RecommendationResult> resultMap = new LinkedHashMap<>();
        List<RecommendationResult> results =
                sessionIds.isEmpty() ? List.of() : recommendationResultMapper.findBySessionIdIn(sessionIds);
        for (RecommendationResult r : results) {
            resultMap.put(r.getSessionId(), r);
        }

        // Batch fetch latest photos (all photos for these sessions, pick latest per session)
        Map<String, Photo> photoMap = new LinkedHashMap<>();
        List<Photo> allPhotos = sessionIds.isEmpty()
                ? List.of()
                : photoMapper.findByRelatedSessionIdInOrderByCreatedAtDesc(sessionIds);
        for (Photo p : allPhotos) {
            photoMap.putIfAbsent(p.getRelatedSessionId(), p);
        }

        // 转换为 RecordListItem 并保持 MP 的分页信息（包装为 spring-data Page 以兼容 controller）
        List<RecordListItem> items = new ArrayList<>();
        for (ConversationSession session : sessions.getRecords()) {
            RecordListItem item = new RecordListItem();
            item.setSessionId(session.getSessionId());
            item.setMode(session.getMode());
            item.setStatus(session.getStatus());
            item.setTotalQuestions(session.getTotalQuestions());
            item.setCurrentQuestionCount(session.getCurrentQuestionCount());
            item.setCreatedAt(session.getCreatedAt());
            item.setCompletedAt(session.getCompletedAt());

            RecommendationResult result = resultMap.get(session.getSessionId());
            if (result != null) {
                item.setFoodName(result.getFoodName());
                item.setReason(result.getReason());
                item.setPhotoUrl(result.getPhotoUrl());
                item.setComment(result.getComment());
                item.setSimilarityScore(result.getSimilarityScore());
            }

            Photo photo = photoMap.get(session.getSessionId());
            if (photo != null) {
                item.setThumbnailUrl(photo.getThumbnailPath());
            }

            if (item.getFoodName() == null || item.getFoodName().isBlank()) {
                item.setFoodName("暂无推荐结果");
            }
            if (item.getReason() == null || item.getReason().isBlank()) {
                item.setReason("该会话暂无可展示的推荐说明");
            }

            items.add(item);
        }

        return new PageImpl<>(items, org.springframework.data.domain.PageRequest.of(page, size),
                sessions.getTotal());
    }

    /**
     * 软删除单条记录：清理 Bloom → 软删关联 FeedPost / 评论 / 物理文件 / QA / 参数 / 推荐 / 会话。
     */
    @Transactional
    public void deleteRecord(String sessionId) {
        log.debug("Soft deleting record: {}", sessionId);
        removeBloomRecommendation(sessionId);
        // 删除关联的 FeedPost + 评论（通过 sessionId 查找）
        FeedPost post = feedPostMapper.findBySessionId(sessionId);
        if (post != null) {
            feedCommentMapper.softDeleteByPostId(post.getId());
            feedPostMapper.softDeleteByPostId(post.getId());
            feedService.cleanRedisForDeletedPost(post.getId(), post.getUserId());
            log.debug("Soft-deleted FeedPost and comments for session {}", sessionId);
        }
        // 删除硬盘上的照片文件
        Photo photo = photoMapper.findFirstByRelatedSessionIdOrderByCreatedAtDesc(sessionId);
        if (photo != null) {
            deletePhysicalFile(photo.getOriginalPath());
            deletePhysicalFile(photo.getThumbnailPath());
            photo.setIsDeleted(1);
            photoMapper.updateById(photo);
        }
        qaRecordMapper.softDeleteBySessionId(sessionId);
        collectedParamMapper.softDeleteBySessionId(sessionId);
        recommendationResultMapper.softDeleteBySessionId(sessionId);
        baseMapper.softDeleteBySessionId(sessionId);
    }

    /**
     * 批量软删除：先批量查 FeedPost / Photo，再 fan-out 软删 + 物理文件清理。
     */
    @Transactional
    public void batchDeleteRecords(List<String> sessionIds) {
        log.info("Batch soft deleting {} records", sessionIds.size());
        for (String sessionId : sessionIds) {
            removeBloomRecommendation(sessionId);
        }

        // 1. Batch fetch all associated feed posts
        List<FeedPost> posts = sessionIds.isEmpty() ? List.of() : feedPostMapper.findBySessionIdIn(sessionIds);
        List<Long> postIds = posts.stream().map(FeedPost::getId).toList();

        // 2. Batch delete comments
        if (!postIds.isEmpty()) {
            feedCommentMapper.softDeleteByPostIdIn(postIds);
        }

        // 3. Batch delete feed posts
        if (!sessionIds.isEmpty()) {
            feedPostMapper.softDeleteBySessionIdIn(sessionIds);
        }

        // 4. Clean Redis for each post (per-post, but already pipelined internally)
        for (FeedPost post : posts) {
            feedService.cleanRedisForDeletedPost(post.getId(), post.getUserId());
        }

        // 5. Batch delete photos (soft delete + file cleanup)
        List<Photo> photos = sessionIds.isEmpty()
                ? List.of()
                : photoMapper.findByRelatedSessionIdInOrderByCreatedAtDesc(sessionIds);
        for (Photo photo : photos) {
            deletePhysicalFile(photo.getOriginalPath());
            deletePhysicalFile(photo.getThumbnailPath());
            photo.setIsDeleted(1);
            photoMapper.updateById(photo);
        }

        // 6. Batch delete remaining entities
        if (!sessionIds.isEmpty()) {
            qaRecordMapper.softDeleteBySessionIdIn(sessionIds);
            collectedParamMapper.softDeleteBySessionIdIn(sessionIds);
            recommendationResultMapper.softDeleteBySessionIdIn(sessionIds);
            baseMapper.softDeleteBySessionIdIn(sessionIds);
        }
    }

    /**
     * 更新推荐结果的照片 URL，并清除该用户的 pending 推荐缓存。
     */
    @Transactional
    public void updateRecommendationPhoto(String sessionId, String photoUrl) {
        log.debug("Updating photo for session: {}", sessionId);
        RecommendationResult r = recommendationResultMapper.findBySessionId(sessionId);
        if (r != null) {
            r.setPhotoUrl(photoUrl);
            recommendationResultMapper.updateById(r);
        }
        ConversationSession session = baseMapper.findBySessionId(sessionId);
        if (session != null) {
            Long userId = session.getUserId();
            if (userId != null) {
                redisTemplate.delete("pending:recommend:" + userId);
            }
        }
    }

    /**
     * 删除推荐结果的照片：清空 photoUrl + 软删 photo 表记录 + 物理文件清理。
     */
    @Transactional
    public void deleteRecommendationPhoto(String sessionId) {
        log.debug("Deleting photo for session: {}", sessionId);
        // 清除 recommendation_result 的 photo_url
        RecommendationResult r = recommendationResultMapper.findBySessionId(sessionId);
        if (r != null) {
            r.setPhotoUrl(null);
            recommendationResultMapper.updateById(r);
        }
        // 软删除 photo 表记录 + 删除硬盘文件
        Photo photo = photoMapper.findFirstByRelatedSessionIdOrderByCreatedAtDesc(sessionId);
        if (photo != null) {
            deletePhysicalFile(photo.getOriginalPath());
            deletePhysicalFile(photo.getThumbnailPath());
            photo.setIsDeleted(1);
            photoMapper.updateById(photo);
        }
    }

    /**
     * 更新推荐结果的用户评价。
     */
    @Transactional
    public void updateComment(String sessionId, String comment) {
        log.info("Updating comment for session: {}", sessionId);
        RecommendationResult r = recommendationResultMapper.findBySessionId(sessionId);
        if (r != null) {
            r.setComment(comment);
            recommendationResultMapper.updateById(r);
        }
    }

    /**
     * 校验 session 归属与时效（超过 30 天的已完成会话不可读）。
     */
    public void validateSessionOwnership(String sessionId, Long userId) {
        ConversationSession session = baseMapper.findBySessionId(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }
        if (!userId.equals(session.getUserId())) {
            throw new RuntimeException("无权访问此会话");
        }
        // 已完成超过 30 天的会话不可读取
        if ("completed".equals(session.getStatus()) && session.getCompletedAt() != null
                && session.getCompletedAt().isBefore(LocalDateTime.now().minusDays(30))) {
            throw new RuntimeException("会话已过期");
        }
    }

    /**
     * 删除物理文件（uploads 子目录）。
     */
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

    /**
     * 在软删除记录前同步移除该记录对应的 Bloom 画像条目。
     */
    private void removeBloomRecommendation(String sessionId) {
        ConversationSession session = baseMapper.findBySessionId(sessionId);
        if (session == null || session.getUserId() == null) {
            return;
        }
        Long userId = session.getUserId();
        RecommendationResult result = recommendationResultMapper.findBySessionId(sessionId);
        if (result != null && result.getId() != null) {
            bloomFilterService.removeRecommendation(userId, result.getId().toString(), null);
        }
    }

    /**
     * 查询某 session 的完整详情：会话 / 推荐 / 已收集参数 / 问答记录 / 照片。
     */
    public RecordDetail getRecordDetail(String sessionId) {
        ConversationSession session = baseMapper.findBySessionId(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }

        Optional<RecommendationResult> optResult =
                Optional.ofNullable(recommendationResultMapper.findBySessionId(sessionId));
        List<CollectedParam> params = collectedParamMapper.findBySessionId(sessionId);
        List<QaRecord> qaRecords = qaRecordMapper.findBySessionIdOrderByQuestionOrderAsc(sessionId);
        Optional<Photo> photo =
                Optional.ofNullable(photoMapper.findFirstByRelatedSessionIdOrderByCreatedAtDesc(sessionId));

        RecordDetail detail = new RecordDetail();
        detail.setSession(session);
        detail.setRecommendation(optResult.orElse(null));
        detail.setCollectedParams(params);
        detail.setQaRecords(qaRecords);
        detail.setPhoto(photo.orElse(null));
        return detail;
    }

    /**
     * 读取 Redis 中缓存的"待发布推荐"sessionId。
     */
    public String getPendingSessionId(Long userId) {
        String cacheKey = "pending:recommend:" + userId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached == null || cached.isBlank()) {
            return null;
        }
        return cached;
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
