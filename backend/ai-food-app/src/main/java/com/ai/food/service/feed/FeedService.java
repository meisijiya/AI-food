package com.ai.food.service.feed;

import com.ai.food.exception.PermissionDeniedException;
import com.ai.food.common.mapper.ChatPhotoMapper;
import com.ai.food.common.mapper.CollectedParamMapper;
import com.ai.food.common.mapper.ConversationSessionMapper;
import com.ai.food.common.mapper.FeedCommentMapper;
import com.ai.food.common.mapper.FeedPostMapper;
import com.ai.food.common.mapper.PhotoMapper;
import com.ai.food.common.mapper.RecommendationResultMapper;
import com.ai.food.common.mapper.UserMapper;
import com.ai.food.common.model.ChatPhoto;
import com.ai.food.common.model.CollectedParam;
import com.ai.food.common.model.ConversationSession;
import com.ai.food.common.model.FeedComment;
import com.ai.food.common.model.FeedPost;
import com.ai.food.common.model.Photo;
import com.ai.food.common.model.RecommendationResult;
import com.ai.food.common.model.SysUser;
import com.ai.food.service.follow.FollowService;
import com.ai.food.service.like.LikeService;
import com.ai.food.service.notification.NotificationService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 动态 Feed 业务服务（MyBatis-Plus 迁移版）。
 * <p>
 * 继承 {@link ServiceImpl} 后，{@code baseMapper} 指向 {@link FeedPostMapper}；
 * 其余实体（评论 / 照片 / 推荐 / 会话 / 用户 / 已收集参数）走注入的 Mapper 字段。
 * </p>
 * <p>
 * 分页在服务边界做 0-based → 1-based 转换，返回给前端的 {@code page} 编号仍维持 0-based。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedService extends ServiceImpl<FeedPostMapper, FeedPost> {

    private final FeedCommentMapper feedCommentMapper;
    private final RecommendationResultMapper recommendationResultMapper;
    private final PhotoMapper photoMapper;
    private final ChatPhotoMapper chatPhotoMapper;
    private final CollectedParamMapper collectedParamMapper;
    private final ConversationSessionMapper conversationSessionMapper;
    private final UserMapper userMapper;
    private final FollowService followService;
    private final NotificationService notificationService;
    private final LikeService likeService;
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

    /**
     * 发布推荐结果到动态。同一 session+user 只能发一次。
     */
    @Transactional
    public Map<String, Object> publishPost(Long userId, String sessionId, String commentPreview, String visibility) {
        // Check if already published
        FeedPost existing = baseMapper.findBySessionIdAndUserId(sessionId, userId);
        if (existing != null) {
            throw new RuntimeException("该推荐结果已发布到动态");
        }

        // Verify session belongs to user
        ConversationSession session = conversationSessionMapper.findBySessionId(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }
        if (!userId.equals(session.getUserId())) {
            throw new RuntimeException("无权发布此推荐结果");
        }

        // Get recommendation result
        RecommendationResult rec = recommendationResultMapper.findBySessionId(sessionId);
        if (rec == null) {
            throw new RuntimeException("推荐结果不存在");
        }

        // Get photo
        Photo optPhoto = photoMapper.findFirstByRelatedSessionIdOrderByCreatedAtDesc(sessionId);

        // Get collected params
        List<CollectedParam> params = collectedParamMapper.findBySessionId(sessionId);

        // Build FeedPost
        FeedPost post = new FeedPost();
        post.setUserId(userId);
        post.setSessionId(sessionId);
        post.setFoodName(rec.getFoodName());
        post.setReason(rec.getReason());
        post.setThumbnailUrl(optPhoto != null ? optPhoto.getThumbnailPath() : null);
        post.setOriginalPhotoUrl(optPhoto != null ? optPhoto.getOriginalPath() : null);
        post.setCommentPreview(commentPreview != null && commentPreview.length() > 30
                ? commentPreview.substring(0, 30) : commentPreview);
        post.setCollectedParams(buildCollectedParamsJson(params));
        post.setVisibility(visibility);
        post.setPublishedAt(LocalDateTime.now());

        // insert 会回填主键 + 写入 createdAt/updatedAt
        baseMapper.insert(post);
        log.debug("Feed post published: postId={}, user={}, visibility={}", post.getId(), userId, visibility);

        // Push to followers' friend feed
        pushToFollowersFeeds(post, userId);

        return buildPostMap(post);
    }

    /**
     * 公共动态流：已登录则过滤"仅粉丝可见"；支持按 foodName / paramValue 模糊搜索。
     */
    public Map<String, Object> getPublicFeedList(int page, int size, String foodName,
                                                  String paramName, String paramValue, Long currentUserId) {
        // MP 分页 1-based，控制器传 0-based
        IPage<FeedPost> postPage = new Page<>(page + 1, size);

        String searchParamValue = null;
        if (paramName != null && paramValue != null) {
            searchParamValue = "\"" + paramName + "\":\"" + paramValue + "\"";
        }

        String searchFoodName = foodName != null && !foodName.isBlank() ? foodName : null;

        if (currentUserId != null) {
            List<Long> followingIds = followService.getFollowingIds(currentUserId);
            if (!followingIds.isEmpty()) {
                postPage = baseMapper.findPublicAndFansOnlyByFilters(
                        postPage, followingIds, searchFoodName, searchParamValue);
            } else {
                postPage = baseMapper.findPublicByFilters(
                        postPage, searchFoodName, searchParamValue);
            }
        } else {
            postPage = baseMapper.findPublicByFilters(
                    postPage, searchFoodName, searchParamValue);
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (FeedPost post : postPage.getRecords()) {
            Map<String, Object> item = buildPostMap(post);
            items.add(item);
        }
        batchEnrichUserInfo(items);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("page", (int) postPage.getCurrent() - 1);
        result.put("size", postPage.getSize());
        result.put("totalElements", postPage.getTotal());
        result.put("totalPages", (int) postPage.getPages());
        return result;
    }

    /**
     * 热榜 Top20：未登录走缓存，登录时按粉丝可见性过滤。
     */
    public Map<String, Object> getHotRank(Long currentUserId) {
        // 优先从缓存获取（仅当未登录时使用缓存，已登录时需要过滤粉丝可见帖子）
        if (currentUserId == null) {
            String cached = stringRedisTemplate.opsForValue().get(HOT_DETAILS_KEY);
            if (cached != null && !cached.isEmpty()) {
                try {
                    return objectMapper.readValue(cached, new TypeReference<Map<String, Object>>() {});
                } catch (JsonProcessingException e) {
                    log.error("Failed to parse hot rank cache", e);
                }
            }
        }

        // 缓存不存在或需要过滤，从数据库加载
        return loadAndCacheHotRank(currentUserId);
    }

    /**
     * 从 Redis ZSet 读出 Top20 帖子 ID 列表 → 数据库加载详情 → 写缓存。
     */
    private Map<String, Object> loadAndCacheHotRank(Long currentUserId) {
        Set<ZSetOperations.TypedTuple<String>> hotPosts = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(HOT_RANK_KEY, 0, 19);

        if (hotPosts == null || hotPosts.isEmpty()) {
            Map<String, Object> emptyResult = Map.of("items", new ArrayList<>());
            if (currentUserId == null) {
                try {
                    stringRedisTemplate.opsForValue().set(HOT_DETAILS_KEY, objectMapper.writeValueAsString(emptyResult));
                } catch (JsonProcessingException e) {
                    log.error("Failed to cache empty hot rank", e);
                }
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
            if (currentUserId == null) {
                try {
                    stringRedisTemplate.opsForValue().set(HOT_DETAILS_KEY, objectMapper.writeValueAsString(emptyResult));
                } catch (JsonProcessingException e) {
                    log.error("Failed to cache empty hot rank", e);
                }
            }
            return emptyResult;
        }

        List<FeedPost> posts = baseMapper.findByIdIn(postIds);
        Map<Long, FeedPost> postMap = new LinkedHashMap<>();
        for (FeedPost post : posts) {
            postMap.put(post.getId(), post);
        }

        // Build following set for visibility filtering
        Set<Long> followingIds = new HashSet<>();
        if (currentUserId != null) {
            followingIds.addAll(followService.getFollowingIds(currentUserId));
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (Long postId : postIds) {
            FeedPost post = postMap.get(postId);
            if (post != null && Integer.valueOf(0).equals(post.getIsDeleted())) {
                // Filter fans-only posts: only show to followers of the author
                if ("friends".equals(post.getVisibility())) {
                    if (currentUserId == null || !followingIds.contains(post.getUserId())) {
                        continue;
                    }
                }
                Map<String, Object> item = buildSimplifiedPostMap(post);
                item.put("hotScore", scoreMap.getOrDefault(postId, 0.0).intValue());
                items.add(item);
            }
        }
        batchEnrichUserInfo(items);

        Map<String, Object> result = Map.of("items", items);

        // 仅未登录时缓存结果
        if (currentUserId == null) {
            try {
                stringRedisTemplate.opsForValue().set(HOT_DETAILS_KEY, objectMapper.writeValueAsString(result));
                log.info("Hot rank cache refreshed, {} items", items.size());
            } catch (JsonProcessingException e) {
                log.error("Failed to cache hot rank", e);
            }
        }

        return result;
    }

    /**
     * 构造简化版帖子 Map（用于热榜卡片展示）。
     */
    private Map<String, Object> buildSimplifiedPostMap(FeedPost post) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", post.getId());
        map.put("userId", post.getUserId());
        map.put("foodName", post.getFoodName());
        map.put("thumbnailUrl", post.getThumbnailUrl());
        map.put("likeCount", post.getLikeCount());
        map.put("commentCount", post.getCommentCount());
        map.put("visibility", post.getVisibility());
        return map;
    }

    /**
     * 检查某 postId 是否在热榜 Top20 中，是则刷新缓存。
     */
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
            loadAndCacheHotRank(null);
        }
    }

    /**
     * 定时刷新热榜缓存（仅未登录分支），每 10 分钟一次。
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void refreshHotRankCacheScheduled() {
        log.info("Scheduled hot rank cache refresh started");
        loadAndCacheHotRank(null);
        log.info("Scheduled hot rank cache refresh completed");
    }

    /**
     * 读取某用户的关注流（已由发布时 fan-out 写入 Redis List）。
     */
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

    /**
     * 发布帖子时 fan-out 到所有粉丝的 Redis 关注流（最多保留最近 100 条）。
     */
    private void pushToFollowersFeeds(FeedPost post, Long userId) {
        List<Long> followerIds = followService.getFollowerIds(userId);
        if (followerIds.isEmpty()) {
            return;
        }

        // Get user info for summary
        String nickname = "匿名用户";
        String avatar = null;
        SysUser user = userMapper.selectById(userId);
        if (user != null) {
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
        summary.put("visibility", post.getVisibility());

        try {
            String summaryJson = objectMapper.writeValueAsString(summary);

            final List<Long> followerIdsFinal = followerIds;
            stringRedisTemplate.executePipelined((RedisConnection connection) -> {
                for (Long followerId : followerIdsFinal) {
                    String key = FRIEND_FEED_KEY + followerId;
                    connection.listCommands().lPush(key.getBytes(), summaryJson.getBytes());
                    connection.listCommands().lTrim(key.getBytes(), 0, 99);
                }
                return null;
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize feed summary", e);
        }
    }

    /**
     * 动态详情：含可见性校验、是否已点赞、关联照片 / 参数、浏览数 +1、命中热榜则刷缓存。
     */
    public Map<String, Object> getFeedDetail(Long postId, Long currentUserId) {
        FeedPost post = baseMapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("动态不存在");
        }
        assertFeedVisible(post, currentUserId);

        Map<String, Object> result = buildPostMap(post);
        enrichUserInfo(result, post.getUserId());

        // Check if current user liked
        Boolean isLiked = currentUserId != null ?
            stringRedisTemplate.opsForSet().isMember(LIKE_SET_KEY + postId, currentUserId.toString()) :
            false;
        result.put("isLiked", Boolean.TRUE.equals(isLiked));

        // Photo info
        Photo optPhoto = photoMapper.findFirstByRelatedSessionIdOrderByCreatedAtDesc(post.getSessionId());
        if (optPhoto != null) {
            result.put("photoUrl", optPhoto.getOriginalPath());
            result.put("thumbnailUrl", optPhoto.getThumbnailPath());
        }

        // Collected params
        List<CollectedParam> params = collectedParamMapper.findBySessionId(post.getSessionId());
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
        baseMapper.updateById(post);

        return result;
    }

    /**
     * 对详情接口重复执行可见性校验，避免绕过列表过滤直接读到受限动态。
     */
    private void assertFeedVisible(FeedPost post, Long currentUserId) {
        if ("public".equals(post.getVisibility())) {
            return;
        }
        if (currentUserId != null && Objects.equals(post.getUserId(), currentUserId)) {
            return;
        }
        if ("friends".equals(post.getVisibility()) && currentUserId != null && followService.isFollowing(currentUserId, post.getUserId())) {
            return;
        }
        throw new PermissionDeniedException("无权查看该动态");
    }

    /**
     * 点赞 / 取消点赞（实际逻辑在 LikeService）。
     */
    @Transactional
    public Map<String, Object> toggleLike(Long postId, Long userId) {
        return likeService.toggleLike(postId, userId);
    }

    /**
     * 添加评论并更新帖子评论计数 + 通知作者 + 命中热榜则刷缓存。
     */
    @Transactional
    public Map<String, Object> addComment(Long postId, Long userId, String content, String imageUrl) {
        FeedPost post = baseMapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("动态不存在");
        }

        FeedComment comment = new FeedComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setImageUrl(imageUrl);
        feedCommentMapper.insert(comment);

        // Increment comment count
        post.setCommentCount(post.getCommentCount() + 1);
        baseMapper.updateById(post);

        // Add notification for post owner
        if (!post.getUserId().equals(userId)) {
            String commenterName = "匿名用户";
            SysUser commenter = userMapper.selectById(userId);
            if (commenter != null) {
                commenterName = commenter.getNickname() != null ? commenter.getNickname() : commenter.getUsername();
            }
            notificationService.addCommentNotification(
                    post.getUserId(), comment.getId(), postId, userId, commenterName, content);
        }

        // Increment hot score for comment (+5)
        stringRedisTemplate.opsForZSet().incrementScore(HOT_RANK_KEY, postId.toString(), 5);
        // 检查是否需要刷新热榜缓存
        checkAndRefreshHotRank(postId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", comment.getId());
        result.put("postId", comment.getPostId());
        result.put("userId", comment.getUserId());
        result.put("content", comment.getContent());
        result.put("imageUrl", comment.getImageUrl());
        result.put("createdAt", comment.getCreatedAt());
        enrichUserInfo(result, comment.getUserId());
        return result;
    }

    /**
     * 获取某帖子的评论分页（按时间倒序）。
     */
    public Map<String, Object> getComments(Long postId, int page, int size) {
        IPage<FeedComment> commentPage = new Page<>(page + 1, size);
        feedCommentMapper.findByPostIdOrderByCreatedAtDesc(commentPage, postId);

        List<Map<String, Object>> items = new ArrayList<>();
        for (FeedComment comment : commentPage.getRecords()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", comment.getId());
            item.put("postId", comment.getPostId());
            item.put("userId", comment.getUserId());
            item.put("content", comment.getContent());
            item.put("imageUrl", comment.getImageUrl());
            item.put("createdAt", comment.getCreatedAt());
            items.add(item);
        }
        batchEnrichUserInfo(items);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("page", (int) commentPage.getCurrent() - 1);
        result.put("size", commentPage.getSize());
        result.put("totalElements", commentPage.getTotal());
        result.put("totalPages", (int) commentPage.getPages());
        return result;
    }

    /**
     * 删除评论：归属校验 → 软删评论 → 同步删除关联 chat_photo → 同步通知/计数 → 热榜扣分。
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        FeedComment comment = feedCommentMapper.findByIdAndUserId(commentId, userId);
        if (comment == null) {
            throw new RuntimeException("评论不存在或无权限删除");
        }
        if (Boolean.TRUE.equals(comment.getIsDeleted())) {
            return;
        }

        int updated = feedCommentMapper.softDeleteByIdAndUserId(commentId, userId);
        if (updated <= 0) {
            return;
        }

        if (comment.getImageUrl() != null && !comment.getImageUrl().isBlank()) {
            ChatPhoto photo = chatPhotoMapper.findByOriginalPath(comment.getImageUrl());
            if (photo == null) {
                photo = chatPhotoMapper.findByThumbnailPath(comment.getImageUrl());
            }
            if (photo != null) {
                photo.setIsDeleted(1);
                chatPhotoMapper.updateById(photo);
            }
        }

        FeedPost postForNotif = baseMapper.selectById(comment.getPostId());
        if (postForNotif != null) {
            notificationService.removeCommentNotification(postForNotif.getUserId(), commentId);
        }

        FeedPost postForCount = baseMapper.selectById(comment.getPostId());
        if (postForCount != null) {
            int currentCount = postForCount.getCommentCount() == null ? 0 : postForCount.getCommentCount();
            postForCount.setCommentCount(Math.max(0, currentCount - 1));
            baseMapper.updateById(postForCount);
        }

        stringRedisTemplate.opsForZSet().incrementScore(HOT_RANK_KEY, comment.getPostId().toString(), -5);
        checkAndRefreshHotRank(comment.getPostId());
    }

    /**
     * 检查某 session 是否已发布到动态，返回可见性。
     */
    public Map<String, Object> checkPublishedWithVisibility(String sessionId, Long userId) {
        FeedPost post = baseMapper.findBySessionIdAndUserId(sessionId, userId);
        Map<String, Object> result = new LinkedHashMap<>();
        if (post != null) {
            result.put("published", true);
            result.put("visibility", post.getVisibility());
        } else {
            result.put("published", false);
            result.put("visibility", null);
        }
        return result;
    }

    /**
     * 简单判断：某 session 是否已被当前用户发布过。
     */
    public boolean checkPublished(String sessionId, Long userId) {
        return baseMapper.findBySessionIdAndUserId(sessionId, userId) != null;
    }

    /**
     * 撤回动态：级联软删评论 + 软删动态 + 清理 Redis + 删除物理文件。
     */
    @Transactional
    public void unpublish(Long userId, String sessionId) {
        FeedPost post = baseMapper.findBySessionIdAndUserId(sessionId, userId);
        if (post == null) {
            throw new RuntimeException("该动态不存在或已被取消");
        }

        // 软删除关联评论
        feedCommentMapper.softDeleteByPostId(post.getId());

        // 软删除动态
        post.setIsDeleted(1);
        baseMapper.updateById(post);

        // Clean up Redis
        cleanRedisForDeletedPost(post.getId(), userId);

        // Clean up physical photo files
        deletePhotoFiles(post.getThumbnailUrl());
        deletePhotoFiles(post.getOriginalPhotoUrl());

        log.debug("Feed post unpublished: postId={}, user={}", post.getId(), userId);
    }

    /**
     * 清理动态相关 Redis：点赞 set、计数、热榜、关注流条目。
     */
    public void cleanRedisForDeletedPost(Long postId, Long userId) {
        try {
            // Clean up like data
            stringRedisTemplate.delete(LIKE_SET_KEY + postId);
            stringRedisTemplate.delete(LIKE_COUNT_KEY + postId);

            // Clean up hot rank
            stringRedisTemplate.opsForZSet().remove(HOT_RANK_KEY, postId.toString());
            // Invalidate hot rank cache
            stringRedisTemplate.delete(HOT_DETAILS_KEY);

            // Clean up friend feed entries for all followers using pipelining
            List<Long> followerIds = followService.getFollowerIds(userId);
            if (!followerIds.isEmpty()) {
                final String postIdStr = postId.toString();
                // Step 1: Pipeline LRANGE for all follower lists
                List<Object> rangeResults = stringRedisTemplate.executePipelined((RedisConnection connection) -> {
                    for (Long fid : followerIds) {
                        String friendKey = FRIEND_FEED_KEY + fid;
                        connection.listCommands().lRange(friendKey.getBytes(), 0, -1);
                    }
                    return null;
                });
                // Step 2: Pipeline LREM for matching entries
                if (rangeResults != null) {
                    final List<Long> followerIdsFinal = followerIds;
                    stringRedisTemplate.executePipelined((RedisConnection connection) -> {
                        for (int i = 0; i < rangeResults.size() && i < followerIdsFinal.size(); i++) {
                            @SuppressWarnings("unchecked")
                            List<String> entries = (List<String>) rangeResults.get(i);
                            if (entries != null) {
                                String friendKey = FRIEND_FEED_KEY + followerIdsFinal.get(i);
                                for (String entry : entries) {
                                    try {
                                        Map<?, ?> map = objectMapper.readValue(entry, Map.class);
                                        if (postIdStr.equals(String.valueOf(map.get("postId")))) {
                                            connection.listCommands().lRem(friendKey.getBytes(), 1, entry.getBytes());
                                        }
                                    } catch (Exception ignored) {}
                                }
                            }
                        }
                        return null;
                    });
                }
            }
        } catch (Exception e) {
            log.warn("Failed to clean Redis for deleted post {}", postId, e);
        }
    }

    /**
     * 删除单张物理照片文件（用于动态撤回）。
     */
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

    /**
     * 把外部累计的点赞数同步回数据库（内部使用）。
     */
    private void updateDbLikeCount(Long postId, int count) {
        FeedPost post = baseMapper.selectById(postId);
        if (post != null) {
            post.setLikeCount(count);
            baseMapper.updateById(post);
        }
    }

    /**
     * 注入单条用户公开信息（昵称 / 头像）。
     */
    private void enrichUserInfo(Map<String, Object> target, Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user != null) {
            target.put("nickname", user.getNickname() != null ? user.getNickname() : "匿名用户");
            target.put("avatar", user.getAvatar());
        }
    }

    /**
     * 批量注入用户公开信息，减少 N+1 查询。
     */
    private void batchEnrichUserInfo(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) return;
        Set<Long> userIds = new LinkedHashSet<>();
        for (Map<String, Object> item : items) {
            Number uid = (Number) item.get("userId");
            if (uid != null) userIds.add(uid.longValue());
        }
        if (userIds.isEmpty()) return;
        Map<Long, SysUser> userMap = new LinkedHashMap<>();
        for (SysUser user : userMapper.findByIdIn(new ArrayList<>(userIds))) {
            userMap.put(user.getId(), user);
        }
        for (Map<String, Object> item : items) {
            Number uid = (Number) item.get("userId");
            if (uid == null) continue;
            SysUser user = userMap.get(uid.longValue());
            if (user != null) {
                item.put("nickname", user.getNickname() != null ? user.getNickname() : "匿名用户");
                item.put("avatar", user.getAvatar());
            }
        }
    }

    /**
     * 构造帖子详情 Map（不含用户信息，由 enrich 方法补充）。
     */
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
        map.put("visibility", post.getVisibility());
        map.put("publishedAt", post.getPublishedAt());
        return map;
    }

    /**
     * 序列化为前端可直接展示的已收集参数 JSON。
     */
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
