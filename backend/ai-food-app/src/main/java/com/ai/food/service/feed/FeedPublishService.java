package com.ai.food.service.feed;

import com.ai.food.common.mapper.CollectedParamMapper;
import com.ai.food.common.mapper.ConversationSessionMapper;
import com.ai.food.common.mapper.FeedCommentMapper;
import com.ai.food.common.mapper.FeedPostMapper;
import com.ai.food.common.mapper.PhotoMapper;
import com.ai.food.common.mapper.RecommendationResultMapper;
import com.ai.food.common.mapper.UserMapper;
import com.ai.food.common.model.CollectedParam;
import com.ai.food.common.model.ConversationSession;
import com.ai.food.common.model.FeedPost;
import com.ai.food.common.model.Photo;
import com.ai.food.common.model.RecommendationResult;
import com.ai.food.common.model.SysUser;
import com.ai.food.service.follow.FollowService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.ai.food.service.feed.FeedUtil.FRIEND_FEED_KEY;
import static com.ai.food.service.feed.FeedUtil.HOT_DETAILS_KEY;
import static com.ai.food.service.feed.FeedUtil.HOT_RANK_KEY;
import static com.ai.food.service.feed.FeedUtil.LIKE_COUNT_KEY;
import static com.ai.food.service.feed.FeedUtil.LIKE_SET_KEY;
import static com.ai.food.service.feed.FeedUtil.buildCollectedParamsJson;
import static com.ai.food.service.feed.FeedUtil.buildPostMap;

/**
 * Feed 发布 / 撤回服务：发布推荐到动态 / fan-out 到粉丝 / 撤回级联清理。
 * <p>ponytail: 保留原 publishPost / unpublish / cleanRedisForDeletedPost 的事务边界与 Redis pipeline 顺序。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedPublishService {

    private final FeedPostMapper feedPostMapper;
    private final FeedCommentMapper feedCommentMapper;
    private final RecommendationResultMapper recommendationResultMapper;
    private final PhotoMapper photoMapper;
    private final CollectedParamMapper collectedParamMapper;
    private final ConversationSessionMapper conversationSessionMapper;
    private final UserMapper userMapper;
    private final FollowService followService;
    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 发布推荐结果到动态。同一 session+user 只能发一次。
     */
    @Transactional
    public Map<String, Object> publishPost(Long userId, String sessionId, String commentPreview, String visibility) {
        // Check if already published
        FeedPost existing = feedPostMapper.findBySessionIdAndUserId(sessionId, userId);
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
        feedPostMapper.insert(post);
        log.debug("Feed post published: postId={}, user={}, visibility={}", post.getId(), userId, visibility);

        // Push to followers' friend feed
        pushToFollowersFeeds(post, userId);

        return buildPostMap(post);
    }

    /**
     * 检查某 session 是否已发布到动态，返回可见性。
     */
    public Map<String, Object> checkPublishedWithVisibility(String sessionId, Long userId) {
        FeedPost post = feedPostMapper.findBySessionIdAndUserId(sessionId, userId);
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
        return feedPostMapper.findBySessionIdAndUserId(sessionId, userId) != null;
    }

    /**
     * 撤回动态：级联软删评论 + 软删动态 + 清理 Redis + 删除物理文件。
     */
    @Transactional
    public void unpublish(Long userId, String sessionId) {
        FeedPost post = feedPostMapper.findBySessionIdAndUserId(sessionId, userId);
        if (post == null) {
            throw new RuntimeException("该动态不存在或已被取消");
        }

        // 软删除关联评论
        feedCommentMapper.softDeleteByPostId(post.getId());

        // 软删除动态
        post.setIsDeleted(1);
        feedPostMapper.updateById(post);

        // Clean up Redis
        cleanRedisForDeletedPost(post.getId(), userId);

        // Clean up physical photo files
        deletePhotoFiles(post.getThumbnailUrl());
        deletePhotoFiles(post.getOriginalPhotoUrl());

        log.debug("Feed post unpublished: postId={}, user={}", post.getId(), userId);
    }

    /**
     * 清理动态相关 Redis：点赞 set、计数、热榜、关注流条目。
     * <p>ponytail: 该方法由 RecordService 也直接调用（外部 API），签名必须保持 public。</p>
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
                                    } catch (Exception ignored) {
                                    }
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
}
