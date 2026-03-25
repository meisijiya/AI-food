package com.ai.food.service.notification;

import com.ai.food.model.SysUser;
import com.ai.food.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;

    private static final String LIST_KEY = "notification:list:";
    private static final String UNREAD_KEY = "notification:unread:";
    private static final String CHAT_KEY = "notification:chat:";
    private static final Duration TTL = Duration.ofDays(7);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // ==================== 评论通知 ====================

    public void addCommentNotification(Long postOwnerId, Long commentId, Long postId,
                                        Long commenterId, String nickname, String content) {
        if (postOwnerId.equals(commenterId)) return;

        try {
            Map<String, Object> notif = new LinkedHashMap<>();
            notif.put("type", "comment");
            notif.put("id", "comment_" + commentId);
            notif.put("postId", postId);
            notif.put("userId", commenterId);
            notif.put("nickname", nickname);
            notif.put("content", content.length() > 50 ? content.substring(0, 50) : content);
            notif.put("timestamp", System.currentTimeMillis());

            String json = OBJECT_MAPPER.writeValueAsString(notif);
            String listKey = LIST_KEY + postOwnerId;
            redisTemplate.opsForZSet().add(listKey, json, System.currentTimeMillis());
            redisTemplate.expire(listKey, TTL);
            incrementUnread(postOwnerId);
            log.debug("Comment notification added: postOwner={}, commenter={}", postOwnerId, commenterId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize comment notification", e);
        }
    }

    // ==================== 聊天通知（聚合） ====================

    public void updateChatNotification(Long receiverId, Long conversationId,
                                        Long senderId, String nickname, String avatar, String message) {
        try {
            Map<String, Object> notif = new LinkedHashMap<>();
            notif.put("type", "chat");
            notif.put("conversationId", conversationId);
            notif.put("senderId", senderId);
            notif.put("nickname", nickname);
            notif.put("avatar", avatar);
            notif.put("lastMessage", message.length() > 50 ? message.substring(0, 50) : message);
            notif.put("timestamp", System.currentTimeMillis());

            String hashKey = CHAT_KEY + receiverId;
            String field = conversationId.toString();
            String json = OBJECT_MAPPER.writeValueAsString(notif);

            // 未读计数：只有 Hash 中已有该 conversation 时才 +1（首次不算，已在 ChatService 的 unread 中计数）
            boolean existed = redisTemplate.opsForHash().hasKey(hashKey, field);
            redisTemplate.opsForHash().put(hashKey, field, json);
            redisTemplate.expire(hashKey, TTL);

            if (existed) {
                // 解析旧的 unreadCount 并 +1
                String oldJson = redisTemplate.opsForHash().get(hashKey, field).toString();
                Map<String, Object> oldNotif = OBJECT_MAPPER.readValue(oldJson, new TypeReference<>() {});
                int count = ((Number) oldNotif.getOrDefault("unreadCount", 0)).intValue() + 1;
                notif.put("unreadCount", count);
                redisTemplate.opsForHash().put(hashKey, field, OBJECT_MAPPER.writeValueAsString(notif));
            }
            // 首次不增加通知未读数（ChatService 已有专门的 chat:unread 管理）
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize chat notification", e);
        }
    }

    // ==================== 查询通知列表 ====================

    public Map<String, Object> getNotifications(Long userId, int page, int size) {
        String listKey = LIST_KEY + userId;
        String hashKey = CHAT_KEY + userId;

        // 从 SortedSet 逆序获取（最新优先）
        long start = (long) page * size;
        long end = start + size - 1;
        Set<String> commentJsons = redisTemplate.opsForZSet().reverseRange(listKey, start, end);

        // 获取评论通知总数
        Long commentTotal = redisTemplate.opsForZSet().size(listKey);

        // 获取 chat 聚合通知（仅第一页时返回）
        List<Map<String, Object>> chatNotifs = new ArrayList<>();
        if (page == 0) {
            Map<Object, Object> chatEntries = redisTemplate.opsForHash().entries(hashKey);
            for (Object value : chatEntries.values()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> item = OBJECT_MAPPER.readValue(value.toString(), Map.class);
                    chatNotifs.add(item);
                } catch (JsonProcessingException e) {
                    log.error("Failed to parse chat notification", e);
                }
            }
            // 按 timestamp 降序
            chatNotifs.sort((a, b) -> {
                long ta = ((Number) a.getOrDefault("timestamp", 0)).longValue();
                long tb = ((Number) b.getOrDefault("timestamp", 0)).longValue();
                return Long.compare(tb, ta);
            });
        }

        // 组装评论通知
        List<Map<String, Object>> commentNotifs = new ArrayList<>();
        if (commentJsons != null) {
            for (String json : commentJsons) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> item = OBJECT_MAPPER.readValue(json, Map.class);
                    // 补充评论者头像
                    Long commenterId = ((Number) item.get("userId")).longValue();
                    userRepository.findById(commenterId).ifPresent(user -> {
                        item.put("avatar", user.getAvatar());
                    });
                    commentNotifs.add(item);
                } catch (JsonProcessingException e) {
                    log.error("Failed to parse comment notification", e);
                }
            }
        }

        // 合并排序：chat（仅第一页）+ comment，按 timestamp 降序
        List<Map<String, Object>> allItems = new ArrayList<>(chatNotifs);
        allItems.addAll(commentNotifs);
        allItems.sort((a, b) -> {
            long ta = ((Number) a.getOrDefault("timestamp", 0)).longValue();
            long tb = ((Number) b.getOrDefault("timestamp", 0)).longValue();
            return Long.compare(tb, ta);
        });

        int unread = getUnreadCount(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", allItems);
        result.put("page", page);
        result.put("size", size);
        result.put("commentTotal", commentTotal != null ? commentTotal : 0);
        result.put("unread", unread);
        return result;
    }

    // ==================== 删除通知 ====================

    public void deleteNotification(Long userId, String notificationId) {
        String listKey = LIST_KEY + userId;

        // 尝试从 SortedSet 中删除（按 id 匹配）
        Set<String> allJsons = redisTemplate.opsForZSet().range(listKey, 0, -1);
        if (allJsons != null) {
            for (String json : allJsons) {
                try {
                    Map<String, Object> item = OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
                    String id = (String) item.get("id");
                    if (notificationId.equals(id)) {
                        redisTemplate.opsForZSet().remove(listKey, json);
                        decrementUnread(userId);
                        return;
                    }
                } catch (JsonProcessingException e) {
                    // skip
                }
            }
        }

        // 尝试从 chat Hash 中删除
        if (notificationId.startsWith("chat_")) {
            String conversationId = notificationId.substring(5);
            String hashKey = CHAT_KEY + userId;
            redisTemplate.opsForHash().delete(hashKey, conversationId);
        }
    }

    public void clearAllNotifications(Long userId) {
        redisTemplate.delete(LIST_KEY + userId);
        redisTemplate.delete(CHAT_KEY + userId);
        redisTemplate.delete(UNREAD_KEY + userId);
        log.info("All notifications cleared for user {}", userId);
    }

    // ==================== 未读计数 ====================

    public int getUnreadCount(Long userId) {
        String val = redisTemplate.opsForValue().get(UNREAD_KEY + userId);
        return val != null ? Integer.parseInt(val) : 0;
    }

    public void decrementUnread(Long userId) {
        Long val = redisTemplate.opsForValue().decrement(UNREAD_KEY + userId);
        if (val == null || val < 0) {
            redisTemplate.opsForValue().set(UNREAD_KEY + userId, "0");
        }
    }

    private void incrementUnread(Long userId) {
        redisTemplate.opsForValue().increment(UNREAD_KEY + userId);
        redisTemplate.expire(UNREAD_KEY + userId, TTL);
    }
}
