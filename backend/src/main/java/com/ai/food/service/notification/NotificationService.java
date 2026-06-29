package com.ai.food.service.notification;

import com.ai.food.mapper.UserMapper;
import com.ai.food.model.SysUser;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 通知中心服务（MyBatis-Plus 迁移版）。
 * <p>
 * 继承 {@link ServiceImpl} 后，{@code baseMapper} 指向 {@link UserMapper}；
 * 业务数据本身存放在 Redis（SortedSet + Hash），仅在补齐评论者头像时按 id 批量查用户表。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService extends ServiceImpl<UserMapper, SysUser> {

    private final StringRedisTemplate redisTemplate;

    private static final String LIST_KEY = "notification:list:";
    private static final String UNREAD_KEY = "notification:unread:";
    private static final String CHAT_KEY = "notification:chat:";
    private static final Duration TTL = Duration.ofDays(7);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // ==================== 评论通知 ====================

    /**
     * 写入一条评论通知到 ZSet（按时间戳打分），并递增全局未读计数。
     */
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

    /**
     * 按 id 匹配删除一条评论通知，并递减未读计数。
     */
    public void removeCommentNotification(Long postOwnerId, Long commentId) {
        String listKey = LIST_KEY + postOwnerId;
        Set<String> allJsons = redisTemplate.opsForZSet().range(listKey, 0, -1);
        if (allJsons == null || allJsons.isEmpty()) {
            return;
        }

        String targetId = "comment_" + commentId;
        for (String json : allJsons) {
            try {
                Map<String, Object> item = OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
                if (targetId.equals(item.get("id"))) {
                    Long removed = redisTemplate.opsForZSet().remove(listKey, json);
                    if (removed != null && removed > 0) {
                        decrementUnread(postOwnerId);
                    }
                    return;
                }
            } catch (JsonProcessingException e) {
                log.error("Failed to parse comment notification during removal", e);
            }
        }
    }

    // ==================== 聊天通知（聚合） ====================

    /**
     * 聊天通知聚合写入：同会话覆盖前一条并把 unreadCount +1。
     */
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

            // 获取旧的 unreadCount
            int unreadCount = 1;
            Object oldVal = redisTemplate.opsForHash().get(hashKey, field);
            if (oldVal != null) {
                try {
                    Map<String, Object> oldNotif = OBJECT_MAPPER.readValue(oldVal.toString(), new TypeReference<>() {});
                    unreadCount = ((Number) oldNotif.getOrDefault("unreadCount", 0)).intValue() + 1;
                } catch (JsonProcessingException e) {
                    // parse error, use default 1
                }
            }
            notif.put("unreadCount", unreadCount);

            redisTemplate.opsForHash().put(hashKey, field, OBJECT_MAPPER.writeValueAsString(notif));
            redisTemplate.expire(hashKey, TTL);

            // 每条消息都给全局通知未读计数加一
            incrementUnread(receiverId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize chat notification", e);
        }
    }

    // ==================== 查询通知列表 ====================

    /**
     * 通知中心首页：第一页合并 chat 聚合 + comment；后续页仅 comment。
     */
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
        List<Long> commenterIds = new ArrayList<>();
        if (commentJsons != null) {
            for (String json : commentJsons) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> item = OBJECT_MAPPER.readValue(json, Map.class);
                    Long commenterId = ((Number) item.get("userId")).longValue();
                    commenterIds.add(commenterId);
                    commentNotifs.add(item);
                } catch (JsonProcessingException e) {
                    log.error("Failed to parse comment notification", e);
                }
            }
        }
        // Batch fetch user avatars
        if (!commenterIds.isEmpty()) {
            Map<Long, String> avatarMap = new LinkedHashMap<>();
            for (SysUser user : baseMapper.findByIdIn(commenterIds)) {
                avatarMap.put(user.getId(), user.getAvatar());
            }
            for (Map<String, Object> item : commentNotifs) {
                Long commenterId = ((Number) item.get("userId")).longValue();
                String avatar = avatarMap.get(commenterId);
                if (avatar != null) {
                    item.put("avatar", avatar);
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

    /**
     * 递减聊天通知 Hash 中指定对话的 unreadCount
     * 当用户打开聊天室标记已读时调用
     */
    public void decrementChatNotificationUnread(Long userId, Long conversationId, int amount) {
        if (amount <= 0) return;
        String hashKey = CHAT_KEY + userId;
        String field = conversationId.toString();

        Object val = redisTemplate.opsForHash().get(hashKey, field);
        if (val == null) return;

        try {
            Map<String, Object> notif = OBJECT_MAPPER.readValue(val.toString(), new TypeReference<>() {});
            int unreadCount = ((Number) notif.getOrDefault("unreadCount", 0)).intValue();
            int newCount = Math.max(0, unreadCount - amount);
            notif.put("unreadCount", newCount);
            redisTemplate.opsForHash().put(hashKey, field, OBJECT_MAPPER.writeValueAsString(notif));
        } catch (JsonProcessingException e) {
            log.error("Failed to update chat notification unread count", e);
        }
    }

    /**
     * 按 id 删除通知（comment_* 走 ZSet，chat_* 走 Hash）。
     */
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

            // 读取未读数，递减全局计数
            Object val = redisTemplate.opsForHash().get(hashKey, conversationId);
            if (val != null) {
                try {
                    Map<String, Object> chatNotif = OBJECT_MAPPER.readValue(val.toString(), new TypeReference<>() {});
                    int unreadCount = ((Number) chatNotif.getOrDefault("unreadCount", 0)).intValue();
                    if (unreadCount > 0) {
                        decrementUnread(userId, unreadCount);
                    }
                } catch (JsonProcessingException e) {
                    // parse error, still delete
                }
            }

            redisTemplate.opsForHash().delete(hashKey, conversationId);
        }
    }

    /**
     * 清空用户所有通知（同时把 chat 聚合的未读从全局计数中扣减）。
     */
    public void clearAllNotifications(Long userId) {
        // 清空前累加聊天通知的未读数，递减全局计数
        String chatKey = CHAT_KEY + userId;
        Map<Object, Object> chatEntries = redisTemplate.opsForHash().entries(chatKey);
        int chatUnreadSum = 0;
        for (Object value : chatEntries.values()) {
            try {
                Map<String, Object> chatNotif = OBJECT_MAPPER.readValue(value.toString(), new TypeReference<>() {});
                chatUnreadSum += ((Number) chatNotif.getOrDefault("unreadCount", 0)).intValue();
            } catch (JsonProcessingException e) {
                // skip
            }
        }
        if (chatUnreadSum > 0) {
            decrementUnread(userId, chatUnreadSum);
        }

        redisTemplate.delete(LIST_KEY + userId);
        redisTemplate.delete(chatKey);
        redisTemplate.delete(UNREAD_KEY + userId);
        log.info("All notifications cleared for user {}", userId);
    }

    // ==================== 未读计数 ====================

    /**
     * 读取全局未读计数（Redis String）。
     */
    public int getUnreadCount(Long userId) {
        String val = redisTemplate.opsForValue().get(UNREAD_KEY + userId);
        return val != null ? Integer.parseInt(val) : 0;
    }

    /**
     * 递减全局未读 1。
     */
    public void decrementUnread(Long userId) {
        decrementUnread(userId, 1);
    }

    /**
     * 递减全局未读 amount；小于 0 时回写 0。
     */
    public void decrementUnread(Long userId, int amount) {
        if (amount <= 0) return;
        Long val = redisTemplate.opsForValue().decrement(UNREAD_KEY + userId, amount);
        if (val == null || val < 0) {
            redisTemplate.opsForValue().set(UNREAD_KEY + userId, "0");
        }
    }

    /**
     * 内部使用：递增全局未读 1。
     */
    private void incrementUnread(Long userId) {
        redisTemplate.opsForValue().increment(UNREAD_KEY + userId);
        redisTemplate.expire(UNREAD_KEY + userId, TTL);
    }
}
