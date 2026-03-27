package com.ai.food.service.like;

import com.ai.food.model.FeedPost;
import com.ai.food.repository.FeedPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeStreamConsumer {

    private final StringRedisTemplate stringRedisTemplate;
    private final FeedPostRepository feedPostRepository;

    private static final String STREAM_KEY = "stream:like:events";
    private static final String CONSUMER_GROUP = "cg:like:consumers";
    private static final String CONSUMER_NAME = "consumer-1";
    private static final int BATCH_SIZE = 100;
    private static final long MAX_BLOCK_MS = 100;

    private final Map<Long, Integer> likeCountDelta = new LinkedHashMap<>();
    private final Map<Long, Set<Long>> postLikedUsers = new LinkedHashMap<>();
    private long lastFlushTime = System.currentTimeMillis();
    private static final long FLUSH_INTERVAL_MS = 100;

    @Scheduled(fixedDelay = 100)
    public void consumeLikeEvents() {
        try {
            StreamReadOptions options = StreamReadOptions.empty()
                    .count(BATCH_SIZE)
                    .block(Duration.ofMillis(MAX_BLOCK_MS));
            
            List<MapRecord<String, Object, Object>> rawRecords = stringRedisTemplate.opsForStream()
                    .read(Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
                            options,
                            StreamOffset.create(STREAM_KEY, ReadOffset.from("0")));

            if (rawRecords == null || rawRecords.isEmpty()) {
                if (shouldFlush()) {
                    flushToDatabase();
                }
                return;
            }

            List<String> recordIds = new ArrayList<>();
            for (MapRecord<String, Object, Object> record : rawRecords) {
                Map<String, String> fields = convertToStringMap(record.getValue());
                String recordId = record.getId().getValue();
                processRecord(fields, recordId);
                recordIds.add(recordId);
            }

            stringRedisTemplate.opsForStream().acknowledge(STREAM_KEY, CONSUMER_GROUP, recordIds.toArray(new String[0]));

            if (shouldFlush()) {
                flushToDatabase();
            }

        } catch (Exception e) {
            log.error("Error consuming like events", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> convertToStringMap(Map<Object, Object> rawMap) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : rawMap.entrySet()) {
            result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        return result;
    }

    private void processRecord(Map<String, String> fields, String recordId) {
        try {
            Long postId = Long.parseLong(fields.get("postId"));
            Long userId = Long.parseLong(fields.get("userId"));
            boolean liked = "1".equals(fields.get("liked"));

            likeCountDelta.merge(postId, liked ? 1 : -1, Integer::sum);

            postLikedUsers.computeIfAbsent(postId, k -> new HashSet<>()).add(userId);

            log.debug("Processed like event: postId={}, userId={}, liked={}, recordId={}", 
                    postId, userId, liked, recordId);
        } catch (Exception e) {
            log.error("Error processing record {}: {}", recordId, e.getMessage());
        }
    }

    private boolean shouldFlush() {
        return System.currentTimeMillis() - lastFlushTime >= FLUSH_INTERVAL_MS 
                && (!likeCountDelta.isEmpty() || !postLikedUsers.isEmpty());
    }

    @Transactional
    public void flushToDatabase() {
        if (likeCountDelta.isEmpty()) {
            return;
        }

        Map<Long, Integer> deltaToFlush = new HashMap<>(likeCountDelta);
        likeCountDelta.clear();

        Map<Long, Set<Long>> usersToFlush = new HashMap<>(postLikedUsers);
        postLikedUsers.clear();

        lastFlushTime = System.currentTimeMillis();

        try {
            List<FeedPost> postsToUpdate = new ArrayList<>();
            
            for (Map.Entry<Long, Integer> entry : deltaToFlush.entrySet()) {
                Long postId = entry.getKey();
                Integer delta = entry.getValue();
                
                Optional<FeedPost> postOpt = feedPostRepository.findById(postId);
                if (postOpt.isPresent()) {
                    FeedPost post = postOpt.get();
                    int newCount = Math.max(0, post.getLikeCount() + delta);
                    post.setLikeCount(newCount);
                    postsToUpdate.add(post);
                }
            }

            if (!postsToUpdate.isEmpty()) {
                feedPostRepository.saveAll(postsToUpdate);
                log.info("Flushed like count updates for {} posts, deltas: {}", 
                        postsToUpdate.size(), deltaToFlush);
            }

        } catch (Exception e) {
            log.error("Error flushing like counts to database, restoring state: {}", deltaToFlush, e);
            likeCountDelta.putAll(deltaToFlush);
        }
    }

    public Map<String, Object> getConsumerStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("pendingDeltas", likeCountDelta.size());
        status.put("pendingUserUpdates", postLikedUsers.size());
        status.put("lastFlushTime", lastFlushTime);
        try {
            var groups = stringRedisTemplate.opsForStream().groups(STREAM_KEY);
            status.put("consumerGroups", groups);
        } catch (Exception e) {
            status.put("consumerGroups", "unavailable");
        }
        return status;
    }
}
