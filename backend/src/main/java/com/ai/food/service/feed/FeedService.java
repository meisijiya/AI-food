package com.ai.food.service.feed;

import com.ai.food.model.*;
import com.ai.food.repository.*;
import com.ai.food.service.follow.FollowService;
import com.ai.food.service.notification.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedService {

    private final FeedPostRepository feedPostRepository;
    private final FeedCommentRepository feedCommentRepository;
    private final RecommendationResultRepository recommendationResultRepository;
    private final PhotoRepository photoRepository;
    private final CollectedParamRepository collectedParamRepository;
    private final ConversationSessionRepository conversationSessionRepository;
    private final UserRepository userRepository;
    private final FollowService followService;
    private final NotificationService notificationService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;

    private static final String LIKE_SET_KEY = "feed:like:";
    private static final String LIKE_COUNT_KEY = "feed:like:count:";
    private static final String LIKE_LOCK_KEY = "feed:like:lock:";
    private static final String UNREAD_LIKES_KEY = "feed:notification:likes:";
    private static final String UNREAD_COMMENTS_KEY = "feed:notification:comments:";
    private static final String HOT_RANK_KEY = "feed:hot:rank";
    private static final String HOT_DETAILS_KEY = "feed:hot:details";
    private static final String FRIEND_FEED_KEY = "feed:friend:";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public Map<String, Object> publishPost(Long userId, String sessionId, String commentPreview, String visibility) {
        // Check if already published
        Optional<FeedPost> existing = feedPostRepository.findBySessionIdAndUserId(sessionId, userId);
        if (existing.isPresent()) {
            throw new RuntimeException("该推荐结果已发布到动态");
        }

        // Verify session belongs to user
        ConversationSession session = conversationSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("会话不存在"));
        if (!userId.equals(session.getUserId())) {
            throw new RuntimeException("无权发布此推荐结果");
        }

        // Get recommendation result
        RecommendationResult rec = recommendationResultRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("推荐结果不存在"));

        // Get photo
        Optional<Photo> optPhoto = photoRepository.findFirstByRelatedSessionIdOrderByCreatedAtDesc(sessionId);

        // Get collected params
        List<CollectedParam> params = collectedParamRepository.findBySessionId(sessionId);

        // Build FeedPost
        FeedPost post = new FeedPost();
        post.setUserId(userId);
        post.setSessionId(sessionId);
        post.setFoodName(rec.getFoodName());
        post.setReason(rec.getReason());
        post.setThumbnailUrl(optPhoto.map(Photo::getThumbnailPath).orElse(null));
        post.setOriginalPhotoUrl(optPhoto.map(Photo::getOriginalPath).orElse(null));
        post.setCommentPreview(commentPreview != null && commentPreview.length() > 30
                ? commentPreview.substring(0, 30) : commentPreview);
        post.setCollectedParams(buildCollectedParamsJson(params));
        post.setVisibility(visibility);
        post.setPublishedAt(LocalDateTime.now());

        FeedPost saved = feedPostRepository.save(post);
        log.debug("Feed post published: postId={}, user={}, visibility={}", saved.getId(), userId, visibility);

        // Push to followers' friend feed
        pushToFollowersFeeds(saved, userId);

        return buildPostMap(saved);
    }

    public Map<String, Object> getPublicFeedList(int page, int size, String foodName, String paramName, String paramValue) {
        Pageable pageable = PageRequest.of(page, size);

        String searchParamValue = null;
        if (paramName != null && paramValue != null) {
            searchParamValue = "\"" + paramName + "\":\"" + paramValue + "\"";
        }

        Page<FeedPost> postPage = feedPostRepository.findPublicByFilters(
                foodName != null && !foodName.isBlank() ? foodName : null,
                searchParamValue,
                pageable);

        List<Map<String, Object>> items = new ArrayList<>();
        for (FeedPost post : postPage.getContent()) {
            Map<String, Object> item = buildPostMap(post);
            enrichUserInfo(item, post.getUserId());
            items.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("page", postPage.getNumber());
        result.put("size", postPage.getSize());
        result.put("totalElements", postPage.getTotalElements());
        result.put("totalPages", postPage.getTotalPages());
        return result;
    }

    public Map<String, Object> getHotRank() {
        // 优先从缓存获取
        String cached = stringRedisTemplate.opsForValue().get(HOT_DETAILS_KEY);
        if (cached != null && !cached.isEmpty()) {
            try {
                return objectMapper.readValue(cached, new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                log.error("Failed to parse hot rank cache", e);
            }
        }

        // 缓存不存在，从数据库加载并缓存
        return loadAndCacheHotRank();
    }

    private Map<String, Object> loadAndCacheHotRank() {
        Set<ZSetOperations.TypedTuple<String>> hotPosts = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(HOT_RANK_KEY, 0, 19);

        if (hotPosts == null || hotPosts.isEmpty()) {
            Map<String, Object> emptyResult = Map.of("items", new ArrayList<>());
            // 缓存空结果，避免频繁查询
            try {
                stringRedisTemplate.opsForValue().set(HOT_DETAILS_KEY, objectMapper.writeValueAsString(emptyResult));
            } catch (JsonProcessingException e) {
                log.error("Failed to cache empty hot rank", e);
            }
            return emptyResult;
        }

        List<Long> postIds = new ArrayList<>();
        Map<Long, Double> scoreMap = new LinkedHashMap<>();
        for (ZSetOperations.TypedTuple<String> tuple : hotPosts) {
            if (tuple.getValue() != null) {
                Long postId = Long.parseLong(tuple.getValue());
                postIds.add(postId);
                scoreMap.put(postId, tuple.getScore() != null ? tuple.getScore() : 0.0);
            }
        }

        if (postIds.isEmpty()) {
            Map<String, Object> emptyResult = Map.of("items", new ArrayList<>());
            try {
                stringRedisTemplate.opsForValue().set(HOT_DETAILS_KEY, objectMapper.writeValueAsString(emptyResult));
            } catch (JsonProcessingException e) {
                log.error("Failed to cache empty hot rank", e);
            }
            return emptyResult;
        }

        List<FeedPost> posts = feedPostRepository.findByIdIn(postIds);
        Map<Long, FeedPost> postMap = new LinkedHashMap<>();
        for (FeedPost post : posts) {
            postMap.put(post.getId(), post);
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (Long postId : postIds) {
            FeedPost post = postMap.get(postId);
            if (post != null && !post.getIsDeleted()) {
                Map<String, Object> item = buildSimplifiedPostMap(post);
                item.put("hotScore", scoreMap.getOrDefault(postId, 0.0).intValue());
                enrichUserInfo(item, post.getUserId());
                items.add(item);
            }
        }

        Map<String, Object> result = Map.of("items", items);

        // 缓存结果
        try {
            stringRedisTemplate.opsForValue().set(HOT_DETAILS_KEY, objectMapper.writeValueAsString(result));
            log.info("Hot rank cache refreshed, {} items", items.size());
        } catch (JsonProcessingException e) {
            log.error("Failed to cache hot rank", e);
        }

        return result;
    }

    private Map<String, Object> buildSimplifiedPostMap(FeedPost post) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", post.getId());
        map.put("userId", post.getUserId());
        map.put("foodName", post.getFoodName());
        map.put("thumbnailUrl", post.getThumbnailUrl());
        map.put("likeCount", post.getLikeCount());
        map.put("commentCount", post.getCommentCount());
        return map;
    }

    private void checkAndRefreshHotRank(Long postId) {
        // 获取当前 Top 20 的 postId 列表
        Set<ZSetOperations.TypedTuple<String>> currentTop = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(HOT_RANK_KEY, 0, 19);

        if (currentTop == null || currentTop.isEmpty()) {
            return;
        }

        // 检查 postId 是否在 Top 20 中
        boolean isInTop20 = false;
        for (ZSetOperations.TypedTuple<String> tuple : currentTop) {
            if (tuple.getValue() != null && tuple.getValue().equals(postId.toString())) {
                isInTop20 = true;
                break;
            }
        }

        // 如果在 Top 20 中，刷新缓存
        if (isInTop20) {
            log.info("Post {} is in Top 20, refreshing hot rank cache", postId);
            loadAndCacheHotRank();
        }
    }

    @Scheduled(cron = "0 */10 * * * ?")  // 每10分钟执行
    public void refreshHotRankCacheScheduled() {
        log.info("Scheduled hot rank cache refresh started");
        loadAndCacheHotRank();
        log.info("Scheduled hot rank cache refresh completed");
    }

    public Map<String, Object> getFriendFeedList(Long userId, int page, int size) {
        String key = FRIEND_FEED_KEY + userId;
        long start = page * size;
        long end = start + size - 1;

        List<String> feedJsons = stringRedisTemplate.opsForList().range(key, start, end);
        if (feedJsons == null || feedJsons.isEmpty()) {
            return Map.of("items", new ArrayList<>(), "total", 0);
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (String json : feedJsons) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> item = objectMapper.readValue(json, Map.class);
                items.add(item);
            } catch (JsonProcessingException e) {
                log.error("Failed to parse friend feed item", e);
            }
        }

        Long total = stringRedisTemplate.opsForList().size(key);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("page", page);
        result.put("size", size);
        result.put("total", total != null ? total : 0);
        return result;
    }

    private void pushToFollowersFeeds(FeedPost post, Long userId) {
        List<Long> followerIds = followService.getFollowerIds(userId);
        if (followerIds.isEmpty()) {
            return;
        }

        // Get user info for summary
        String nickname = "匿名用户";
        String avatar = null;
        Optional<SysUser> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            SysUser user = userOpt.get();
            nickname = user.getNickname() != null ? user.getNickname() : user.getUsername();
            avatar = user.getAvatar();
        }

        // Build minimal summary
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("postId", post.getId());
        summary.put("userId", post.getUserId());
        summary.put("foodName", post.getFoodName());
        summary.put("thumbnailUrl", post.getThumbnailUrl());
        summary.put("nickname", nickname);
        summary.put("avatar", avatar);
        summary.put("publishedAt", post.getPublishedAt().toString());

        try {
            String summaryJson = objectMapper.writeValueAsString(summary);

            for (Long followerId : followerIds) {
                String key = FRIEND_FEED_KEY + followerId;
                // Push from left (newest first)
                stringRedisTemplate.opsForList().leftPush(key, summaryJson);
                // Keep max 100 items
                stringRedisTemplate.opsForList().trim(key, 0, 99);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize feed summary", e);
        }
    }

    public Map<String, Object> getFeedDetail(Long postId, Long currentUserId) {
        FeedPost post = feedPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("动态不存在"));

        Map<String, Object> result = buildPostMap(post);
        enrichUserInfo(result, post.getUserId());

        // Check if current user liked
        Boolean isLiked = stringRedisTemplate.opsForSet().isMember(LIKE_SET_KEY + postId, currentUserId.toString());
        result.put("isLiked", Boolean.TRUE.equals(isLiked));

        // Photo info
        Optional<Photo> optPhoto = photoRepository.findFirstByRelatedSessionIdOrderByCreatedAtDesc(post.getSessionId());
        if (optPhoto.isPresent()) {
            result.put("photoUrl", optPhoto.get().getOriginalPath());
            result.put("thumbnailUrl", optPhoto.get().getThumbnailPath());
        }

        // Collected params
        List<CollectedParam> params = collectedParamRepository.findBySessionId(post.getSessionId());
        List<Map<String, String>> paramList = new ArrayList<>();
        for (CollectedParam p : params) {
            Map<String, String> pm = new LinkedHashMap<>();
            pm.put("name", p.getParamName());
            pm.put("value", p.getParamValue());
            paramList.add(pm);
        }
        result.put("collectedParams", paramList);

        // Increment hot score for view
        stringRedisTemplate.opsForZSet().incrementScore(HOT_RANK_KEY, postId.toString(), 1);
        // 检查是否需要刷新热榜缓存
        checkAndRefreshHotRank(postId);

        // Update view count in DB
        post.setViewCount(post.getViewCount() + 1);
        feedPostRepository.save(post);

        return result;
    }

    @Transactional
    public Map<String, Object> toggleLike(Long postId, Long userId) {
        RLock lock = redissonClient.getLock(LIKE_LOCK_KEY + postId + ":" + userId);
        lock.lock();

        try {
            String key = LIKE_SET_KEY + postId;
            String countKey = LIKE_COUNT_KEY + postId;
            String userIdStr = userId.toString();

            Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, userIdStr);
            boolean liked;

            if (Boolean.TRUE.equals(isMember)) {
                // Unlike
                stringRedisTemplate.opsForSet().remove(key, userIdStr);
                Long newCount = stringRedisTemplate.opsForValue().decrement(countKey);
                if (newCount == null || newCount < 0) {
                    newCount = 0L;
                    stringRedisTemplate.opsForValue().set(countKey, "0");
                }
                updateDbLikeCount(postId, newCount.intValue());
                liked = false;
            } else {
                // Like
                stringRedisTemplate.opsForSet().add(key, userIdStr);
                Long newCount = stringRedisTemplate.opsForValue().increment(countKey);
                updateDbLikeCount(postId, newCount.intValue());
                liked = true;

                // Add to unread likes notification for post owner
                FeedPost post = feedPostRepository.findById(postId).orElse(null);
                if (post != null && !post.getUserId().equals(userId)) {
                    stringRedisTemplate.opsForValue().increment(UNREAD_LIKES_KEY + post.getUserId());
                }

                // Increment hot score for like (+3)
                stringRedisTemplate.opsForZSet().incrementScore(HOT_RANK_KEY, postId.toString(), 3);
                // 检查是否需要刷新热榜缓存
                checkAndRefreshHotRank(postId);
            }

            FeedPost post = feedPostRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("动态不存在"));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("liked", liked);
            result.put("likeCount", post.getLikeCount());
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Transactional
    public Map<String, Object> addComment(Long postId, Long userId, String content, String imageUrl) {
        FeedPost post = feedPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("动态不存在"));

        FeedComment comment = new FeedComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setImageUrl(imageUrl);
        FeedComment saved = feedCommentRepository.save(comment);

        // Increment comment count
        post.setCommentCount(post.getCommentCount() + 1);
        feedPostRepository.save(post);

        // Add notification for post owner
        if (!post.getUserId().equals(userId)) {
            String commenterName = userRepository.findById(userId)
                    .map(u -> u.getNickname() != null ? u.getNickname() : u.getUsername())
                    .orElse("匿名用户");
            notificationService.addCommentNotification(
                    post.getUserId(), saved.getId(), postId, userId, commenterName, content);
        }

        // Increment hot score for comment (+5)
        stringRedisTemplate.opsForZSet().incrementScore(HOT_RANK_KEY, postId.toString(), 5);
        // 检查是否需要刷新热榜缓存
        checkAndRefreshHotRank(postId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", saved.getId());
        result.put("postId", saved.getPostId());
        result.put("userId", saved.getUserId());
        result.put("content", saved.getContent());
        result.put("imageUrl", saved.getImageUrl());
        result.put("createdAt", saved.getCreatedAt());
        enrichUserInfo(result, saved.getUserId());
        return result;
    }

    public Map<String, Object> getComments(Long postId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<FeedComment> commentPage = feedCommentRepository.findByPostIdOrderByCreatedAtDesc(postId, pageable);

        List<Map<String, Object>> items = new ArrayList<>();
        for (FeedComment comment : commentPage.getContent()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", comment.getId());
            item.put("postId", comment.getPostId());
            item.put("userId", comment.getUserId());
            item.put("content", comment.getContent());
            item.put("imageUrl", comment.getImageUrl());
            item.put("createdAt", comment.getCreatedAt());
            enrichUserInfo(item, comment.getUserId());
            items.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("page", commentPage.getNumber());
        result.put("size", commentPage.getSize());
        result.put("totalElements", commentPage.getTotalElements());
        result.put("totalPages", commentPage.getTotalPages());
        return result;
    }

    public boolean checkPublished(String sessionId, Long userId) {
        return feedPostRepository.findBySessionIdAndUserId(sessionId, userId).isPresent();
    }

    @Transactional
    public void unpublish(Long userId, String sessionId) {
        FeedPost post = feedPostRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("该动态不存在或已被取消"));

        // 软删除关联评论
        feedCommentRepository.softDeleteByPostId(post.getId());

        // 软删除动态
        post.setIsDeleted(true);
        feedPostRepository.save(post);

        // Clean up Redis like data
        String likeKey = LIKE_SET_KEY + post.getId();
        String countKey = LIKE_COUNT_KEY + post.getId();
        stringRedisTemplate.delete(likeKey);
        stringRedisTemplate.delete(countKey);

        // Clean up Redis hot rank
        stringRedisTemplate.opsForZSet().remove(HOT_RANK_KEY, post.getId().toString());
        stringRedisTemplate.opsForHash().delete(HOT_DETAILS_KEY, post.getId().toString());

        // Clean up friend feed entries for all followers
        try {
            List<Long> followerIds = followService.getFollowerIds(userId);
            for (Long followerId : followerIds) {
                String friendKey = FRIEND_FEED_KEY + followerId;
                List<String> entries = stringRedisTemplate.opsForList().range(friendKey, 0, -1);
                if (entries != null) {
                    for (String entry : entries) {
                        try {
                            Map<?, ?> map = objectMapper.readValue(entry, Map.class);
                            if (post.getId().toString().equals(String.valueOf(map.get("postId")))) {
                                stringRedisTemplate.opsForList().remove(friendKey, 1, entry);
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to clean friend feed entries for post {}", post.getId(), e);
        }

        // Clean up physical photo files
        deletePhotoFiles(post.getThumbnailUrl());
        deletePhotoFiles(post.getOriginalPhotoUrl());

        log.debug("Feed post unpublished: postId={}, user={}", post.getId(), userId);
    }

    private void deletePhotoFiles(String url) {
        if (url == null || url.isBlank()) return;
        try {
            // Extract file path from URL (e.g., /uploads/photos/xxx.jpg)
            String path = url.startsWith("/") ? url : "/" + url;
            java.io.File file = new java.io.File("." + path);
            if (file.exists()) file.delete();
        } catch (Exception e) {
            log.warn("Failed to delete photo file: {}", url, e);
        }
    }

    private void updateDbLikeCount(Long postId, int count) {
        feedPostRepository.findById(postId).ifPresent(post -> {
            post.setLikeCount(count);
            feedPostRepository.save(post);
        });
    }

    private void enrichUserInfo(Map<String, Object> target, Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            target.put("nickname", user.getNickname() != null ? user.getNickname() : "匿名用户");
            target.put("avatar", user.getAvatar());
        });
    }

    private Map<String, Object> buildPostMap(FeedPost post) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", post.getId());
        map.put("userId", post.getUserId());
        map.put("foodName", post.getFoodName());
        map.put("commentPreview", post.getCommentPreview());
        map.put("thumbnailUrl", post.getThumbnailUrl());
        map.put("originalPhotoUrl", post.getOriginalPhotoUrl());
        map.put("reason", post.getReason());
        map.put("collectedParams", post.getCollectedParams());
        map.put("likeCount", post.getLikeCount());
        map.put("commentCount", post.getCommentCount());
        map.put("publishedAt", post.getPublishedAt());
        return map;
    }

    private String buildCollectedParamsJson(List<CollectedParam> params) {
        List<Map<String, String>> list = new ArrayList<>();
        for (CollectedParam p : params) {
            Map<String, String> pm = new LinkedHashMap<>();
            pm.put("name", p.getParamName());
            pm.put("value", p.getParamValue());
            list.add(pm);
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize collected params", e);
            return "[]";
        }
    }
}
