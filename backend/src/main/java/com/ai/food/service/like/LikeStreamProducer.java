package com.ai.food.service.like;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamInfo;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeStreamProducer {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String STREAM_KEY = "stream:like:events";
    private static final String CONSUMER_GROUP = "cg:like:consumers";
    private static final String CONSUMER_NAME = "consumer-1";

    @PostConstruct
    public void init() {
        try {
            stringRedisTemplate.opsForStream().createGroup(STREAM_KEY, CONSUMER_GROUP);
        } catch (Exception e) {
            log.debug("Consumer group already exists or stream not ready: {}", e.getMessage());
        }
        log.info("LikeStreamProducer initialized");
    }

    public void sendLikeEvent(Long postId, Long userId, boolean liked) {
        Map<String, String> event = new HashMap<>();
        event.put("postId", postId.toString());
        event.put("userId", userId.toString());
        event.put("liked", liked ? "1" : "0");
        event.put("timestamp", String.valueOf(System.currentTimeMillis()));
        event.put("type", "like");

        MapRecord<String, String, String> record = MapRecord.create(STREAM_KEY, event);
        RecordId recordId = stringRedisTemplate.opsForStream().add(record);
        
        if (recordId != null) {
            log.debug("Sent like event to stream: postId={}, userId={}, liked={}, recordId={}", 
                    postId, userId, liked, recordId);
        }
    }

    public void sendUnlikeEvent(Long postId, Long userId) {
        Map<String, String> event = new HashMap<>();
        event.put("postId", postId.toString());
        event.put("userId", userId.toString());
        event.put("liked", "0");
        event.put("timestamp", String.valueOf(System.currentTimeMillis()));
        event.put("type", "unlike");

        MapRecord<String, String, String> record = MapRecord.create(STREAM_KEY, event);
        RecordId recordId = stringRedisTemplate.opsForStream().add(record);
        
        if (recordId != null) {
            log.debug("Sent unlike event to stream: postId={}, userId={}, recordId={}", 
                    postId, userId, recordId);
        }
    }

    public void sendBatchEvents(List<LikeEvent> events) {
        for (LikeEvent event : events) {
            sendLikeEvent(event.postId(), event.userId(), event.liked());
        }
    }

    public record LikeEvent(Long postId, Long userId, boolean liked, long timestamp) {
        public static LikeEvent fromMap(Map<String, String> map) {
            return new LikeEvent(
                    Long.parseLong(map.get("postId")),
                    Long.parseLong(map.get("userId")),
                    "1".equals(map.get("liked")),
                    Long.parseLong(map.get("timestamp"))
            );
        }
    }
}
