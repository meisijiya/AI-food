package com.ai.food.service.chat;

import com.ai.food.service.follow.FollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import static com.ai.food.service.chat.ChatUtil.MAX_NON_MUTUAL_MESSAGES;
import static com.ai.food.service.chat.ChatUtil.MSG_COUNT_KEY;

/**
 * 聊天发送权限服务：互关判定 + 非互关 5 条上限计数。
 * <p>
 * 提取自原 {@code ChatService}（752 行），按 fix-7 FeedService 拆分同模式：单一职责，便于独立单测。
 * </p>
 *
 * <p>ponytail: 不引入新抽象（接口/工厂）；Redis 计数逻辑与原行为完全一致，未改写 Lua 或字符串拼接。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatPermissionService {

    private final FollowService followService;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 检查发送权限
     * @return "ok"=可以发送, "max_reached"=已达上限, "not_allowed"=不允许发送
     */
    public String checkSendPermission(Long senderId, Long receiverId) {
        // 1. 互关 → 直接放行
        if (followService.isMutualFollow(senderId, receiverId)) {
            return "ok";
        }

        boolean senderFollowsReceiver = followService.isFollowing(senderId, receiverId);
        boolean receiverFollowsSender = followService.isFollowing(receiverId, senderId);

        // 2. 任一方向关注 → 检查消息计数（各自独立计数）
        if (senderFollowsReceiver || receiverFollowsSender) {
            try {
                String countKey = MSG_COUNT_KEY + senderId + ":" + receiverId;
                String countStr = stringRedisTemplate.opsForValue().get(countKey);
                int count = countStr != null ? Integer.parseInt(countStr) : 0;
                return count >= MAX_NON_MUTUAL_MESSAGES ? "max_reached" : "ok";
            } catch (Exception e) {
                log.warn("Redis read failed for msg count, defaulting to ok: {}", e.getMessage());
                return "ok";
            }
        }

        // 3. 无任何关注关系 → 拒绝
        return "not_allowed";
    }

    /**
     * 获取剩余可发送条数（用于前端展示）
     */
    public int getRemainingMessages(Long senderId, Long receiverId) {
        String perm = checkSendPermission(senderId, receiverId);
        if (perm.equals("ok") && followService.isMutualFollow(senderId, receiverId)) {
            return -1;
        }
        if (perm.equals("not_allowed")) return 0;

        try {
            String countKey = MSG_COUNT_KEY + senderId + ":" + receiverId;
            String countStr = stringRedisTemplate.opsForValue().get(countKey);
            int count = countStr != null ? Integer.parseInt(countStr) : 0;
            return Math.max(0, MAX_NON_MUTUAL_MESSAGES - count);
        } catch (Exception e) {
            log.warn("Redis read failed for remaining count: {}", e.getMessage());
            return MAX_NON_MUTUAL_MESSAGES;
        }
    }

    /**
     * 非互关消息计数 +1（在 sendMessage 写入新消息成功后调用）。
     * <p>ponytail: 与 read 在同一个 service 里，MSG_COUNT_KEY 整个 key 命名空间只在本 service 出现。</p>
     */
    public void incrementMessageCount(Long senderId, Long receiverId) {
        try {
            String countKey = MSG_COUNT_KEY + senderId + ":" + receiverId;
            stringRedisTemplate.opsForValue().increment(countKey);
        } catch (Exception e) {
            log.warn("Redis increment failed for msg count: {}", e.getMessage());
        }
    }
}
