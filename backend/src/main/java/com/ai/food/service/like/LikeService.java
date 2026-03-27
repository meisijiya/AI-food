package com.ai.food.service.like;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeService {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<Long> toggleLikeScript;
    private final HeavyKeeperService heavyKeeperService;
    private final LikeStreamProducer likeStreamProducer;
    private final Cache<Long, Long> hotPostLikeCache;
    private final Cache<String, Boolean> hotPostLikeStatusCache;

    private static final String LIKE_SET_KEY = "feed:like:";
    private static final String LIKE_COUNT_KEY = "feed:like:count:";
    private static final long HOT_THRESHOLD = 10;

    public Map<String, Object> toggleLike(Long postId, Long userId) {
        String setKey = LIKE_SET_KEY + postId;
        String countKey = LIKE_COUNT_KEY + postId;

        Long result = stringRedisTemplate.execute(
                toggleLikeScript,
                Collections.singletonList(setKey),
                userId.toString(),
                countKey
        );

        boolean liked = result != null && result == 1L;
        boolean unliked = result != null && result == -1L;

        if (liked) {
            heavyKeeperService.recordAccess(postId);
            likeStreamProducer.sendLikeEvent(postId, userId, true);
        } else if (unliked) {
            heavyKeeperService.recordUnAccess(postId);
            likeStreamProducer.sendUnlikeEvent(postId, userId);
        }

        if (heavyKeeperService.isHotPost(postId)) {
            Long count = getLikeCountFromRedis(postId);
            hotPostLikeCache.put(postId, count);
        }

        hotPostLikeStatusCache.put(postId + ":" + userId, liked);

        long likeCount = getLikeCount(postId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("liked", liked);
        response.put("likeCount", likeCount);
        response.put("queued", true);
        return response;
    }

    public boolean isLiked(Long postId, Long userId) {
        String statusKey = postId + ":" + userId;
        Boolean cached = hotPostLikeStatusCache.getIfPresent(statusKey);
        if (cached != null) {
            return cached;
        }

        String setKey = LIKE_SET_KEY + postId;
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(setKey, userId.toString());
        boolean result = Boolean.TRUE.equals(isMember);
        hotPostLikeStatusCache.put(statusKey, result);
        return result;
    }

    public long getLikeCount(Long postId) {
        if (heavyKeeperService.isHotPost(postId)) {
            Long cached = hotPostLikeCache.getIfPresent(postId);
            if (cached != null) {
                return cached;
            }
        }

        return getLikeCountFromRedis(postId);
    }

    public long getLikeCountFromRedis(Long postId) {
        String countKey = LIKE_COUNT_KEY + postId;
        String count = stringRedisTemplate.opsForValue().get(countKey);
        return count != null ? Long.parseLong(count) : 0L;
    }

    public void syncLikeCountToCache(Long postId) {
        if (heavyKeeperService.isHotPost(postId)) {
            long count = getLikeCountFromRedis(postId);
            hotPostLikeCache.put(postId, count);
        }
    }

    public List<Long> getHotPosts(int limit) {
        return heavyKeeperService.getTopKHotPosts(limit);
    }

    public void invalidateCache(Long postId, Long userId) {
        hotPostLikeCache.invalidate(postId);
        hotPostLikeStatusCache.invalidate(postId + ":" + userId);
    }

    public void invalidateAllCache() {
        hotPostLikeCache.invalidateAll();
        hotPostLikeStatusCache.invalidateAll();
    }

    public Map<String, Object> getLikeStatus(Long postId, Long userId) {
        boolean liked = isLiked(postId, userId);
        long count = getLikeCount(postId);
        boolean isHot = heavyKeeperService.isHotPost(postId);

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("postId", postId);
        status.put("liked", liked);
        status.put("likeCount", count);
        status.put("isHot", isHot);
        return status;
    }
}
