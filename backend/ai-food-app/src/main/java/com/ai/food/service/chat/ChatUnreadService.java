package com.ai.food.service.chat;

import com.ai.food.common.mapper.UserMapper;
import com.ai.food.common.model.SysUser;
import com.ai.food.service.follow.FollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.ai.food.service.chat.ChatUtil.ONLINE_KEY;
import static com.ai.food.service.chat.ChatUtil.UNREAD_KEY;
import static com.ai.food.service.chat.ChatUtil.UNREAD_TOTAL_KEY;

/**
 * 聊天未读计数 + 在线状态服务（纯 Redis 状态层）。
 * <p>
 * 提取自原 {@code ChatService}（752 行），按 fix-7 FeedService 拆分同模式：把 hash/string key 的
 * 增量/清零/读取与 5 分钟在线 TTL 集中到一个 service，便于独立单测 Redis 状态机。
 * </p>
 *
 * <p>ponytail: 不引入新抽象；{@code getUnreadMap} / {@code incrementUnread} / {@code clearUnread}
 * 对外 public 是因为 {@link ChatMessageService} 内部要复用（保持 Redis hash + 全局计数的两步一致性）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatUnreadService {

    private final StringRedisTemplate stringRedisTemplate;
    private final FollowService followService;
    private final UserMapper userMapper;

    /**
     * 接收方未读 +1（hash 按会话分桶 + 全局计数）。
     */
    public void incrementUnread(Long userId, Long conversationId) {
        try {
            String key = UNREAD_KEY + userId;
            stringRedisTemplate.opsForHash().increment(key, conversationId.toString(), 1);
            stringRedisTemplate.opsForValue().increment(UNREAD_TOTAL_KEY + userId, 1);
        } catch (Exception e) {
            log.warn("Redis increment unread failed: userId={}, convId={}: {}", userId, conversationId, e.getMessage());
        }
    }

    /**
     * 用户打开会话后清零该会话未读（同时扣减全局未读）。
     */
    public void clearUnread(Long userId, Long conversationId) {
        try {
            String key = UNREAD_KEY + userId;
            Object count = stringRedisTemplate.opsForHash().get(key, conversationId.toString());
            if (count != null) {
                int countVal = Integer.parseInt(count.toString());
                stringRedisTemplate.opsForHash().delete(key, conversationId.toString());

                Long total = stringRedisTemplate.opsForValue().decrement(UNREAD_TOTAL_KEY + userId, countVal);
                if (total == null || total < 0) {
                    stringRedisTemplate.opsForValue().set(UNREAD_TOTAL_KEY + userId, "0");
                }
            }
        } catch (Exception e) {
            log.warn("Redis clear unread failed: userId={}, convId={}: {}", userId, conversationId, e.getMessage());
        }
    }

    /**
     * 读取用户各会话的未读 hash。
     */
    public Map<Long, Integer> getUnreadMap(Long userId) {
        String key = UNREAD_KEY + userId;
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            result.put(Long.parseLong(entry.getKey().toString()), Integer.parseInt(entry.getValue().toString()));
        }
        return result;
    }

    /**
     * 获取用户总未读数（合并各会话未读）。
     */
    public Map<String, Object> getUnreadCounts(Long userId) {
        Map<Long, Integer> unreadMap = getUnreadMap(userId);
        int totalUnread = unreadMap.values().stream().mapToInt(Integer::intValue).sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalUnread", totalUnread);
        result.put("conversations", unreadMap);
        return result;
    }

    /**
     * 获取用户总未读（直接从 Redis 全局 key 读取）。
     */
    public int getTotalUnreadCount(Long userId) {
        String totalStr = stringRedisTemplate.opsForValue().get(UNREAD_TOTAL_KEY + userId);
        return totalStr != null ? Integer.parseInt(totalStr) : 0;
    }

    /**
     * 获取用户的互关好友列表（用于发起聊天时的联系人面板）。
     */
    public List<Map<String, Object>> getContacts(Long userId) {
        List<Long> mutualFriendIds = followService.getMutualFriendIds(userId);
        List<Map<String, Object>> result = new ArrayList<>();

        if (mutualFriendIds.isEmpty()) return result;

        // Batch fetch users
        Map<Long, SysUser> userMap = new LinkedHashMap<>();
        for (SysUser user : userMapper.findByIdIn(mutualFriendIds)) {
            userMap.put(user.getId(), user);
        }

        // Pipeline online status checks
        final List<Long> friendIds = mutualFriendIds;
        List<Object> onlineResults = stringRedisTemplate.executePipelined((RedisConnection connection) -> {
            for (Long friendId : friendIds) {
                connection.keyCommands().exists((ONLINE_KEY + friendId).getBytes());
            }
            return null;
        });

        for (int i = 0; i < friendIds.size(); i++) {
            Long friendId = friendIds.get(i);
            SysUser user = userMap.get(friendId);
            if (user != null) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("userId", user.getId());
                item.put("nickname", user.getNickname() != null ? user.getNickname() : user.getUsername());
                item.put("avatar", user.getAvatar());
                item.put("isOnline", i < onlineResults.size() && onlineResults.get(i) instanceof Boolean && (Boolean) onlineResults.get(i));
                result.add(item);
            }
        }

        return result;
    }

    /**
     * 标记用户在线（5 分钟 TTL，由前端定期续期）。
     */
    public void setUserOnline(Long userId) {
        stringRedisTemplate.opsForValue().set(ONLINE_KEY + userId, "1", java.time.Duration.ofMinutes(5));
    }

    /**
     * 用户主动下线或服务下线时清除在线标记。
     */
    public void setUserOffline(Long userId) {
        stringRedisTemplate.delete(ONLINE_KEY + userId);
    }

    /**
     * 判断用户是否在线（Redis 标记存在）。
     */
    public boolean isOnline(Long userId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(ONLINE_KEY + userId));
    }
}
