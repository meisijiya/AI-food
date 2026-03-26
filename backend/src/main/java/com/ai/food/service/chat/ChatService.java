package com.ai.food.service.chat;

import com.ai.food.model.ChatConversation;
import com.ai.food.model.ChatMessage;
import com.ai.food.model.SysUser;
import com.ai.food.repository.ChatConversationRepository;
import com.ai.food.repository.ChatFileRepository;
import com.ai.food.repository.ChatMessageRepository;
import com.ai.food.repository.ChatPhotoRepository;
import com.ai.food.repository.UserRepository;
import com.ai.food.service.follow.FollowService;
import com.ai.food.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatPhotoRepository chatPhotoRepository;
    private final ChatFileRepository chatFileRepository;
    private final UserRepository userRepository;
    private final FollowService followService;
    private final NotificationService notificationService;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String UNREAD_KEY = "chat:unread:";
    private static final String UNREAD_TOTAL_KEY = "chat:unread:total:";
    private static final String ONLINE_KEY = "chat:online:";
    private static final String MSG_COUNT_KEY = "chat:msgcount:";
    private static final int MAX_NON_MUTUAL_MESSAGES = 5;

    /**
     * 检查发送权限
     * @return "ok"=可以发送, "max_reached"=已达上限, "not_allowed"=不允许发送
     */
    public String checkSendPermission(Long senderId, Long receiverId) {
        if (followService.isMutualFollow(senderId, receiverId)) {
            return "ok";
        }

        boolean senderFollowsReceiver = followService.isFollowing(senderId, receiverId);
        boolean receiverFollowsSender = followService.isFollowing(receiverId, senderId);

        if (senderFollowsReceiver && !receiverFollowsSender) {
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

    @Transactional
    public ChatMessage sendMessage(Long senderId, Long receiverId, String content, String messageType) {
        String permission = checkSendPermission(senderId, receiverId);
        if (permission.equals("not_allowed")) {
            throw new RuntimeException("对方未关注你，无法发送消息");
        }
        if (permission.equals("max_reached")) {
            throw new RuntimeException("非互关最多发送" + MAX_NON_MUTUAL_MESSAGES + "条消息");
        }

        if (!followService.isMutualFollow(senderId, receiverId)) {
            try {
                String countKey = MSG_COUNT_KEY + senderId + ":" + receiverId;
                stringRedisTemplate.opsForValue().increment(countKey);
            } catch (Exception e) {
                log.warn("Redis increment failed for msg count: {}", e.getMessage());
            }
        }

        ChatConversation conversation = getOrCreateConversation(senderId, receiverId);

        // 重置接收方的 hiddenAt — 会话重新出现在列表，但 clearedAt 不变（旧消息仍被过滤）
        resetHiddenForReceiver(conversation, receiverId);

        ChatMessage message = new ChatMessage();
        message.setConversationId(conversation.getId());
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setMessageType(messageType != null ? messageType : "text");
        message.setIsRead(false);
        ChatMessage saved = messageRepository.save(message);

        conversation.setLastMessage(content.length() > 50 ? content.substring(0, 50) + "..." : content);
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        incrementUnread(receiverId, conversation.getId());

        String senderName = "匿名用户";
        String senderAvatar = null;
        Optional<SysUser> senderOpt = userRepository.findById(senderId);
        if (senderOpt.isPresent()) {
            SysUser sender = senderOpt.get();
            senderName = sender.getNickname() != null ? sender.getNickname() : sender.getUsername();
            senderAvatar = sender.getAvatar();
        }
        notificationService.updateChatNotification(receiverId, conversation.getId(),
                senderId, senderName, senderAvatar, content);

        log.info("Message sent: from={} to={}, conversationId={}", senderId, receiverId, conversation.getId());
        return saved;
    }

    /**
     * 重置接收方的 hiddenAt，使会话在收到新消息后重新出现在聊天列表
     * 注意：clearedAt 保持不变，确保旧消息继续被过滤不可见
     */
    private void resetHiddenForReceiver(ChatConversation conversation, Long receiverId) {
        if (conversation.getUser1Id().equals(receiverId) && conversation.getHiddenAtUser1() != null) {
            conversationRepository.resetHiddenAtUser1(conversation.getId());
            conversation.setHiddenAtUser1(null);
        } else if (conversation.getUser2Id().equals(receiverId) && conversation.getHiddenAtUser2() != null) {
            conversationRepository.resetHiddenAtUser2(conversation.getId());
            conversation.setHiddenAtUser2(null);
        }
    }

    public ChatConversation getOrCreateConversation(Long userId1, Long userId2) {
        String key = ChatConversation.generateKey(userId1, userId2);
        return conversationRepository.findByConversationKey(key)
                .orElseGet(() -> {
                    ChatConversation conversation = new ChatConversation();
                    conversation.setConversationKey(key);
                    conversation.setUser1Id(Math.min(userId1, userId2));
                    conversation.setUser2Id(Math.max(userId1, userId2));
                    return conversationRepository.save(conversation);
                });
    }

    /**
     * 获取或创建与指定用户的对话，返回包含 conversationId 的 Map
     */
    public Map<String, Object> getOrCreateConversationWith(Long userId, Long otherUserId) {
        ChatConversation conv = getOrCreateConversation(userId, otherUserId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conversationId", conv.getId());
        result.put("userId", otherUserId);

        userRepository.findById(otherUserId).ifPresent(user -> {
            result.put("nickname", user.getNickname() != null ? user.getNickname() : user.getUsername());
            result.put("avatar", user.getAvatar());
        });

        return result;
    }

    public List<Map<String, Object>> getConversationList(Long userId) {
        List<ChatConversation> conversations = conversationRepository.findByUserIdOrderByLastMessageAtDesc(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        Map<Long, Integer> unreadMap = getUnreadMap(userId);

        for (ChatConversation conv : conversations) {
            Long otherUserId = conv.getUser1Id().equals(userId) ? conv.getUser2Id() : conv.getUser1Id();
            userRepository.findById(otherUserId).ifPresent(user -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("conversationId", conv.getId());
                item.put("userId", user.getId());
                item.put("nickname", user.getNickname() != null ? user.getNickname() : user.getUsername());
                item.put("avatar", user.getAvatar());
                item.put("lastMessage", conv.getLastMessage());
                item.put("lastMessageAt", conv.getLastMessageAt());
                item.put("unreadCount", unreadMap.getOrDefault(conv.getId(), 0));
                result.add(item);
            });
        }

        return result;
    }

    /**
     * 获取聊天历史
     * - 使用 clearedAt 过滤消息（clearedAt 永不重置，确保旧消息永久对用户不可见）
     * - @Where 注解自动排除 isDeleted=true 的消息
     */
    @Transactional
    public Map<String, Object> getChatHistory(Long conversationId, Long userId, int page, int size) {
        ChatConversation conv = conversationRepository.findById(conversationId).orElse(null);
        LocalDateTime clearedAt = getClearedAtForUser(conv, userId);

        Pageable pageable = PageRequest.of(page, size);
        Page<ChatMessage> messagePage;

        if (clearedAt != null) {
            // 只返回清除时间点之后的消息（clearedAt 永不重置）
            messagePage = messageRepository.findByConversationIdAfterOrderByCreatedAtDesc(conversationId, clearedAt, pageable);
        } else {
            messagePage = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (ChatMessage msg : messagePage.getContent()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", msg.getId());
            item.put("conversationId", msg.getConversationId());
            item.put("senderId", msg.getSenderId());
            item.put("receiverId", msg.getReceiverId());
            item.put("content", msg.getContent());
            item.put("messageType", msg.getMessageType());
            item.put("isRead", msg.getIsRead());
            item.put("createdAt", msg.getCreatedAt());
            items.add(item);
        }

        markAsRead(userId, conversationId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("page", messagePage.getNumber());
        result.put("size", messagePage.getSize());
        result.put("totalElements", messagePage.getTotalElements());
        result.put("totalPages", messagePage.getTotalPages());
        return result;
    }

    @Transactional
    public void markAsRead(Long userId, Long conversationId) {
        int updated = messageRepository.markAsRead(conversationId, userId);
        if (updated > 0) {
            clearUnread(userId, conversationId);

            // 同步递减全局通知未读计数
            notificationService.decrementUnread(userId, updated);

            // 同步更新通知中心的聊天未读计数
            notificationService.decrementChatNotificationUnread(userId, conversationId, updated);

            log.info("Marked {} messages as read: userId={}, conversationId={}", updated, userId, conversationId);
        }
    }

    public Map<String, Object> getUnreadCounts(Long userId) {
        Map<Long, Integer> unreadMap = getUnreadMap(userId);
        int totalUnread = unreadMap.values().stream().mapToInt(Integer::intValue).sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalUnread", totalUnread);
        result.put("conversations", unreadMap);
        return result;
    }

    public int getTotalUnreadCount(Long userId) {
        String totalStr = stringRedisTemplate.opsForValue().get(UNREAD_TOTAL_KEY + userId);
        return totalStr != null ? Integer.parseInt(totalStr) : 0;
    }

    public List<Map<String, Object>> getContacts(Long userId) {
        List<Long> mutualFriendIds = followService.getMutualFriendIds(userId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Long friendId : mutualFriendIds) {
            userRepository.findById(friendId).ifPresent(user -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("userId", user.getId());
                item.put("nickname", user.getNickname() != null ? user.getNickname() : user.getUsername());
                item.put("avatar", user.getAvatar());
                item.put("isOnline", isOnline(friendId));
                result.add(item);
            });
        }

        return result;
    }

    // ==================== 清除聊天 ====================

    /**
     * 清除聊天记录（clearedAt + hiddenAt 双时间戳方案）
     *
     * clearedAt: 消息过滤边界，永不重置 → 旧消息永久不可见
     * hiddenAt:  列表可见性，新消息到达时重置 → 会话可重新出现
     *
     * 流程：
     * 1. 同时设置 clearedAt 和 hiddenAt 为当前时间
     * 2. 软删除 clearedAt 之前的消息
     * 3. 清理 Redis 未读计数
     * 4. 更新 lastMessage
     * 5. 如果双方都已清除，硬删除软删除记录
     */
    @Transactional
    public void clearConversation(Long userId, Long conversationId) {
        ChatConversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("对话不存在"));

        LocalDateTime now = LocalDateTime.now();

        // 同时设置 clearedAt 和 hiddenAt
        if (conv.getUser1Id().equals(userId)) {
            conversationRepository.setClearedAndHiddenAtUser1(conversationId, now);
        } else {
            conversationRepository.setClearedAndHiddenAtUser2(conversationId, now);
        }

        // 软删除清除时间点之前的消息（@Where 注解自动排除这些消息）
        messageRepository.softDeleteByConversationIdBefore(conversationId, now);
        chatPhotoRepository.softDeleteByConversationIdBefore(conversationId, now);
        chatFileRepository.softDeleteByConversationIdBefore(conversationId, now);

        // 清理 Redis 未读计数
        clearUnread(userId, conversationId);

        // 更新 lastMessage（清除后应为空或仅显示新消息）
        List<ChatMessage> remaining = messageRepository.findLastMessageByConversationId(conversationId, PageRequest.of(0, 1));
        if (remaining.isEmpty()) {
            conversationRepository.updateLastMessage(conversationId, null, conv.getLastMessageAt());
        } else {
            ChatMessage last = remaining.get(0);
            String preview = last.getContent().length() > 50 ? last.getContent().substring(0, 50) + "..." : last.getContent();
            conversationRepository.updateLastMessage(conversationId, preview, last.getCreatedAt());
        }

        // 检查双方是否都已清除 — 如果是，立即硬删除软删除记录
        conv = conversationRepository.findById(conversationId).orElse(conv);
        boolean bothCleared = conv.getClearedAtUser1() != null && conv.getClearedAtUser2() != null;
        if (bothCleared) {
            hardDeleteClearedMessages(conversationId);
            log.info("Both users cleared, hard-deleted soft-deleted records for conversation {}", conversationId);
        }

        log.info("Conversation {} cleared by user {} at {}", conversationId, userId, now);
    }

    /**
     * 硬删除指定对话中所有已软删除的消息、照片和文件
     */
    @Transactional
    public void hardDeleteClearedMessages(Long conversationId) {
        messageRepository.hardDeleteByConversationId(conversationId);
        chatPhotoRepository.hardDeleteByConversationId(conversationId);
        chatFileRepository.hardDeleteByConversationId(conversationId);
    }

    // ==================== Redis 操作 ====================

    private void incrementUnread(Long userId, Long conversationId) {
        try {
            String key = UNREAD_KEY + userId;
            stringRedisTemplate.opsForHash().increment(key, conversationId.toString(), 1);
            stringRedisTemplate.opsForValue().increment(UNREAD_TOTAL_KEY + userId, 1);
        } catch (Exception e) {
            log.warn("Redis increment unread failed: userId={}, convId={}: {}", userId, conversationId, e.getMessage());
        }
    }

    private void clearUnread(Long userId, Long conversationId) {
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

    private Map<Long, Integer> getUnreadMap(Long userId) {
        String key = UNREAD_KEY + userId;
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            result.put(Long.parseLong(entry.getKey().toString()), Integer.parseInt(entry.getValue().toString()));
        }
        return result;
    }

    public void setUserOnline(Long userId) {
        stringRedisTemplate.opsForValue().set(ONLINE_KEY + userId, "1", java.time.Duration.ofMinutes(5));
    }

    public void setUserOffline(Long userId) {
        stringRedisTemplate.delete(ONLINE_KEY + userId);
    }

    public boolean isOnline(Long userId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(ONLINE_KEY + userId));
    }

    private LocalDateTime getClearedAtForUser(ChatConversation conv, Long userId) {
        if (conv == null) return null;
        if (conv.getUser1Id().equals(userId)) {
            return conv.getClearedAtUser1();
        } else {
            return conv.getClearedAtUser2();
        }
    }
}
