package com.ai.food.service.chat;

import com.ai.food.model.ChatConversation;
import com.ai.food.model.ChatMessage;
import com.ai.food.model.SysUser;
import com.ai.food.repository.ChatConversationRepository;
import com.ai.food.repository.ChatMessageRepository;
import com.ai.food.repository.UserRepository;
import com.ai.food.service.follow.FollowService;
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
    private final UserRepository userRepository;
    private final FollowService followService;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String UNREAD_KEY = "chat:unread:";
    private static final String UNREAD_TOTAL_KEY = "chat:unread:total:";
    private static final String ONLINE_KEY = "chat:online:";

    @Transactional
    public ChatMessage sendMessage(Long senderId, Long receiverId, String content, String messageType) {
        // 获取或创建对话
        ChatConversation conversation = getOrCreateConversation(senderId, receiverId);

        // 创建消息
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversation.getId());
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setMessageType(messageType != null ? messageType : "text");
        message.setIsRead(false);
        ChatMessage saved = messageRepository.save(message);

        // 更新对话的最后消息
        conversation.setLastMessage(content.length() > 50 ? content.substring(0, 50) + "..." : content);
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // 更新 Redis 未读计数
        incrementUnread(receiverId, conversation.getId());

        log.info("Message sent: from={} to={}, conversationId={}", senderId, receiverId, conversation.getId());
        return saved;
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

    public List<Map<String, Object>> getConversationList(Long userId) {
        List<ChatConversation> conversations = conversationRepository.findByUserIdOrderByLastMessageAtDesc(userId);
        List<Map<String, Object>> result = new ArrayList<>();

        // 获取未读计数
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

    @Transactional
    public Map<String, Object> getChatHistory(Long conversationId, Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatMessage> messagePage = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);

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

        // 标记为已读
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

    // ==================== Redis 操作 ====================

    private void incrementUnread(Long userId, Long conversationId) {
        String key = UNREAD_KEY + userId;
        stringRedisTemplate.opsForHash().increment(key, conversationId.toString(), 1);
        stringRedisTemplate.opsForValue().increment(UNREAD_TOTAL_KEY + userId, 1);
    }

    private void clearUnread(Long userId, Long conversationId) {
        String key = UNREAD_KEY + userId;
        Object count = stringRedisTemplate.opsForHash().get(key, conversationId.toString());
        if (count != null) {
            int countVal = Integer.parseInt(count.toString());
            stringRedisTemplate.opsForHash().delete(key, conversationId.toString());
            
            // 更新总未读数
            Long total = stringRedisTemplate.opsForValue().decrement(UNREAD_TOTAL_KEY + userId, countVal);
            if (total == null || total < 0) {
                stringRedisTemplate.opsForValue().set(UNREAD_TOTAL_KEY + userId, "0");
            }
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

    private void enrichUserInfo(Map<String, Object> target, Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            target.put("nickname", user.getNickname() != null ? user.getNickname() : user.getUsername());
            target.put("avatar", user.getAvatar());
        });
    }
}
