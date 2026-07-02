package com.ai.food.service.feed;

import com.ai.food.common.mapper.FeedPostMapper;
import com.ai.food.common.mapper.UserMapper;
import com.ai.food.common.model.FeedPost;
import com.ai.food.service.follow.FollowService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.ai.food.service.feed.FeedUtil.HOT_DETAILS_KEY;
import static com.ai.food.service.feed.FeedUtil.HOT_RANK_KEY;
import static com.ai.food.service.feed.FeedUtil.OBJECT_MAPPER;
import static com.ai.food.service.feed.FeedUtil.batchEnrichUserInfo;
import static com.ai.food.service.feed.FeedUtil.buildSimplifiedPostMap;

/**
 * 热榜管理服务：从 Redis ZSet 读取 Top20 → 加载详情 → 写缓存；并为查询/评论服务提供分数更新 + 缓存刷新。
 * <p>ponytail: 仅持有原 {@link FeedService} 中与 Redis ZSet / HeavyKeeper 集成相关的逻辑，不重写 Redis / 缓存协议。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedHotRankService {

    private final FeedPostMapper feedPostMapper;
    private final UserMapper userMapper;
    private final FollowService followService;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 从 Redis ZSet 读出 Top20 帖子 ID 列表 → 数据库加载详情 → 写缓存。
     */
    public Map<String, Object> loadAndCacheHotRank(Long currentUserId) {
        Set<ZSetOperations.TypedTuple<String>> hotPosts = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(HOT_RANK_KEY, 0, 19);

        if (hotPosts == null || hotPosts.isEmpty()) {
            Map<String, Object> emptyResult = Map.of("items", new ArrayList<>());
            if (currentUserId == null) {
                try {
                    FeedUtil.cacheHotDetailsWithTtl(stringRedisTemplate, OBJECT_MAPPER.writeValueAsString(emptyResult));
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
                    FeedUtil.cacheHotDetailsWithTtl(stringRedisTemplate, OBJECT_MAPPER.writeValueAsString(emptyResult));
                } catch (JsonProcessingException e) {
                    log.error("Failed to cache empty hot rank", e);
                }
            }
            return emptyResult;
        }

        List<FeedPost> posts = feedPostMapper.findByIdIn(postIds);
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
        batchEnrichUserInfo(items, userMapper);

        Map<String, Object> result = Map.of("items", items);

        // 仅未登录时缓存结果
        if (currentUserId == null) {
            try {
                FeedUtil.cacheHotDetailsWithTtl(stringRedisTemplate, OBJECT_MAPPER.writeValueAsString(result));
                log.info("Hot rank cache refreshed, {} items", items.size());
            } catch (JsonProcessingException e) {
                log.error("Failed to cache hot rank", e);
            }
        }

        return result;
    }

    /**
     * 检查某 postId 是否在热榜 Top20 中，是则刷新缓存。
     */
    public void checkAndRefreshHotRank(Long postId) {
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
     * 调整热榜分数并在必要时刷新缓存（被 FeedQueryService.getFeedDetail / FeedCommentService 调用）。
     *
     * @param postId 帖子 ID
     * @param delta  分数增量（点赞 +1 / 评论 +5 / 删评论 -5 / 浏览 +1 等）
     */
    public void incrementHotScore(Long postId, double delta) {
        stringRedisTemplate.opsForZSet().incrementScore(HOT_RANK_KEY, postId.toString(), delta);
        checkAndRefreshHotRank(postId);
    }

    /**
     * 读取缓存热榜的辅助方法，供 FeedQueryService.getHotRank 复用，行为保持原样。
     */
    Map<String, Object> readHotRankCache() {
        String cached = stringRedisTemplate.opsForValue().get(HOT_DETAILS_KEY);
        if (cached == null || cached.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(cached, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse hot rank cache", e);
            return null;
        }
    }
}
