package com.ai.food.service.feed;

import com.ai.food.model.*;
import com.ai.food.repository.*;
import com.ai.food.service.follow.FollowService;
import com.fasterxml.jackson.core.JsonProcessingException;
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
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;

    private static final String LIKE_SET_KEY = "feed:like:";
    private static final String LIKE_COUNT_KEY = "feed:like:count:";
    private static final String LIKE_LOCK_KEY = "feed:like:lock:";
    private static final String UNREAD_LIKES_KEY = "feed:notification:likes:";
    private static final String UNREAD_COMMENTS_KEY = "feed:notification:comments:";
    private static final String HOT_RANK_KEY = "feed:hot:rank";
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
        log.info("Feed post published: postId={}, user={}, session={}, visibility={}", saved.getId(), userId, sessionId, visibility);

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
        Set<ZSetOperations.TypedTuple<String>> hotPosts = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(HOT_RANK_KEY, 0, 19);

        if (hotPosts == null || hotPosts.isEmpty()) {
            return Map.of("items", new ArrayList<>());
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
            return Map.of("items", new ArrayList<>());
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
                Map<String, Object> item = buildPostMap(post);
                item.put("hotScore", scoreMap.getOrDefault(postId, 0.0).intValue());
                enrichUserInfo(item, post.getUserId());
                items.add(item);
            }
        }

        return Map.of("items", items);
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
    public Map<String, Object> addComment(Long postId, Long userId, String content) {
        FeedPost post = feedPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("动态不存在"));

        FeedComment comment = new FeedComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        FeedComment saved = feedCommentRepository.save(comment);

        // Increment comment count
        post.setCommentCount(post.getCommentCount() + 1);
        feedPostRepository.save(post);

        // Add to unread comments notification for post owner
        if (!post.getUserId().equals(userId)) {
            stringRedisTemplate.opsForValue().increment(UNREAD_COMMENTS_KEY + post.getUserId());
        }

        // Increment hot score for comment (+5)
        stringRedisTemplate.opsForZSet().incrementScore(HOT_RANK_KEY, postId.toString(), 5);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", saved.getId());
        result.put("postId", saved.getPostId());
        result.put("userId", saved.getUserId());
        result.put("content", saved.getContent());
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

    public Map<String, Object> getNotifications(Long userId) {
        String likesStr = stringRedisTemplate.opsForValue().get(UNREAD_LIKES_KEY + userId);
        String commentsStr = stringRedisTemplate.opsForValue().get(UNREAD_COMMENTS_KEY + userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("unreadLikes", likesStr != null ? Integer.parseInt(likesStr) : 0);
        result.put("unreadComments", commentsStr != null ? Integer.parseInt(commentsStr) : 0);
        return result;
    }

    public void markNotificationsRead(Long userId) {
        stringRedisTemplate.opsForValue().set(UNREAD_LIKES_KEY + userId, "0");
        stringRedisTemplate.opsForValue().set(UNREAD_COMMENTS_KEY + userId, "0");
    }

    public boolean checkPublished(String sessionId, Long userId) {
        return feedPostRepository.findBySessionIdAndUserId(sessionId, userId).isPresent();
    }

    @Transactional
    public void unpublish(Long userId, String sessionId) {
        FeedPost post = feedPostRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("该动态不存在或已被取消"));

        post.setIsDeleted(true);
        feedPostRepository.save(post);

        // Clean up Redis like data
        String likeKey = LIKE_SET_KEY + post.getId();
        String countKey = LIKE_COUNT_KEY + post.getId();
        stringRedisTemplate.delete(likeKey);
        stringRedisTemplate.delete(countKey);

        log.info("Feed post unpublished: postId={}, user={}, session={}", post.getId(), userId, sessionId);
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
        map.put("sessionId", post.getSessionId());
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
