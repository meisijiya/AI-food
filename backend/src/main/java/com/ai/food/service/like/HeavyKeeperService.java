package com.ai.food.service.like;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HeavyKeeperService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String HK_LIKE_COUNT_KEY = "hk:like:count";
    private static final String HK_LIKE_DECAY_KEY = "hk:like:decay";
    private static final String HK_LIKE_TIMESTAMP_KEY = "hk:like:timestamp";

    private static final double DECAY_FACTOR = 0.9;
    private static final long DECAY_INTERVAL_MS = 60000;
    private static final int TOP_K_DEFAULT = 100;
    private static final long HOT_THRESHOLD = 10;

    private final Set<Long> localHotPosts = Collections.newSetFromMap(new LinkedHashMap<Long, Boolean>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Boolean> eldest) {
            return size() > TOP_K_DEFAULT;
        }
    });

    @PostConstruct
    public void init() {
        log.info("HeavyKeeperService initialized");
    }

    public void recordAccess(Long postId) {
        String postIdStr = postId.toString();
        stringRedisTemplate.opsForZSet().incrementScore(HK_LIKE_COUNT_KEY, postIdStr, 1);
        stringRedisTemplate.opsForZSet().incrementScore(HK_LIKE_DECAY_KEY, postIdStr, 1);
        stringRedisTemplate.opsForValue().set(HK_LIKE_TIMESTAMP_KEY + ":" + postIdStr, 
                String.valueOf(System.currentTimeMillis()));
    }

    public void recordUnAccess(Long postId) {
        String postIdStr = postId.toString();
        stringRedisTemplate.opsForZSet().incrementScore(HK_LIKE_COUNT_KEY, postIdStr, -0.5);
    }

    public boolean isHotPost(Long postId) {
        if (localHotPosts.contains(postId)) {
            return true;
        }
        Double score = stringRedisTemplate.opsForZSet().score(HK_LIKE_DECAY_KEY, postId.toString());
        return score != null && score >= HOT_THRESHOLD;
    }

    public List<Long> getTopKHotPosts(int k) {
        Set<ZSetOperations.TypedTuple<String>> topK = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(HK_LIKE_DECAY_KEY, 0, k - 1);
        if (topK == null || topK.isEmpty()) {
            return Collections.emptyList();
        }
        return topK.stream()
                .filter(t -> t.getValue() != null && t.getScore() != null && t.getScore() >= HOT_THRESHOLD)
                .map(t -> Long.parseLong(t.getValue()))
                .collect(Collectors.toList());
    }

    @Scheduled(fixedRate = 60000)
    public void decayHotScores() {
        try {
            Set<ZSetOperations.TypedTuple<String>> all = stringRedisTemplate.opsForZSet()
                    .rangeWithScores(HK_LIKE_DECAY_KEY, 0, -1);
            if (all == null || all.isEmpty()) {
                return;
            }

            long now = System.currentTimeMillis();
            List<ZSetOperations.TypedTuple<String>> toUpdate = new ArrayList<>();
            List<String> toRemove = new ArrayList<>();

            for (ZSetOperations.TypedTuple<String> tuple : all) {
                if (tuple.getValue() == null || tuple.getScore() == null) {
                    continue;
                }
                String postIdStr = tuple.getValue();
                double score = tuple.getScore();

                String timestampStr = stringRedisTemplate.opsForValue()
                        .get(HK_LIKE_TIMESTAMP_KEY + ":" + postIdStr);
                if (timestampStr != null) {
                    long lastUpdate = Long.parseLong(timestampStr);
                    if (now - lastUpdate > DECAY_INTERVAL_MS) {
                        double newScore = score * DECAY_FACTOR;
                        if (newScore < 0.1) {
                            toRemove.add(postIdStr);
                        } else {
                            toUpdate.add(
                                stringRedisTemplate.opsForZSet().add(HK_LIKE_DECAY_KEY, postIdStr, newScore) ?
                                tuple : null
                            );
                        }
                    }
                }
            }

            if (!toRemove.isEmpty()) {
                stringRedisTemplate.opsForZSet().remove(HK_LIKE_DECAY_KEY, toRemove.toArray());
                log.debug("Decay removed {} posts", toRemove.size());
            }

            localHotPosts.clear();
            List<Long> newHotPosts = getTopKHotPosts(TOP_K_DEFAULT);
            localHotPosts.addAll(newHotPosts);
            
        } catch (Exception e) {
            log.error("Error during decay operation", e);
        }
    }

    public Map<Long, Double> getHotPostsWithScores(int k) {
        Set<ZSetOperations.TypedTuple<String>> topK = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(HK_LIKE_DECAY_KEY, 0, k - 1);
        if (topK == null || topK.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Double> result = new LinkedHashMap<>();
        for (ZSetOperations.TypedTuple<String> tuple : topK) {
            if (tuple.getValue() != null && tuple.getScore() != null) {
                result.put(Long.parseLong(tuple.getValue()), tuple.getScore());
            }
        }
        return result;
    }
}
