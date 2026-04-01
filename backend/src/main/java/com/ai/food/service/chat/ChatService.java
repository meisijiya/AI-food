package com.ai.food.service.chat;

import com.ai.food.exception.PermissionDeniedException;
import com.ai.food.exception.ResourceNotFoundException;
import com.ai.food.exception.BusinessException;
import com.ai.food.model.ChatConversation;
import com.ai.food.model.ChatMessage;
import com.ai.food.model.SysUser;
import com.ai.food.model.ChatFile;
import com.ai.food.model.ChatPhoto;
import com.ai.food.repository.ChatConversationRepository;
import com.ai.food.repository.ChatFileRepository;
import com.ai.food.repository.ChatMessageRepository;
import com.ai.food.repository.ChatPhotoRepository;
import com.ai.food.repository.UserRepository;
import com.ai.food.service.follow.FollowService;
import com.ai.food.service.notification.NotificationService;
import com.ai.food.service.upload.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
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
    private final FileUploadService fileUploadService;

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

    @Transactional
    public ChatMessage sendMessage(Long senderId, Long receiverId, String content, String messageType, Long photoId, Long fileId) {
        String permission = checkSendPermission(senderId, receiverId);
        if (permission.equals("not_allowed")) {
            throw new PermissionDeniedException("对方未关注你，无法发送消息");
        }
        if (permission.equals("max_reached")) {
            throw new BusinessException("非互关最多发送" + MAX_NON_MUTUAL_MESSAGES + "条消息");
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
        message.setPhotoId(photoId);
        message.setFileId(fileId);
        message.setIsRead(false);
        ChatMessage saved = messageRepository.save(message);

        // 更新 chat_photo 和 chat_file 的 conversationId
        if (photoId != null) {
            chatPhotoRepository.updateConversationId(photoId, conversation.getId());
        }
        if (fileId != null) {
            chatFileRepository.updateConversationId(fileId, conversation.getId());
        }

        // 根据消息类型设置 lastMessage
        String lastMessagePreview;
        if ("image".equals(messageType)) {
            lastMessagePreview = "【照片】";
        } else if ("file".equals(messageType)) {
            lastMessagePreview = "【文件】";
        } else {
            lastMessagePreview = content.length() > 50 ? content.substring(0, 50) + "..." : content;
        }
        conversation.setLastMessage(lastMessagePreview);
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

        if (!followService.isMutualFollow(senderId, receiverId)) {
            try {
                String countKey = MSG_COUNT_KEY + senderId + ":" + receiverId;
                stringRedisTemplate.opsForValue().increment(countKey);
            } catch (Exception e) {
                log.warn("Redis increment failed for msg count: {}", e.getMessage());
            }
        }

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

        // Collect all otherUserIds and batch fetch
        List<Long> otherUserIds = new ArrayList<>();
        for (ChatConversation conv : conversations) {
            otherUserIds.add(conv.getUser1Id().equals(userId) ? conv.getUser2Id() : conv.getUser1Id());
        }
        Map<Long, SysUser> userMap = new LinkedHashMap<>();
        for (SysUser user : userRepository.findByIdIn(otherUserIds)) {
            userMap.put(user.getId(), user);
        }

        for (ChatConversation conv : conversations) {
            Long otherUserId = conv.getUser1Id().equals(userId) ? conv.getUser2Id() : conv.getUser1Id();
            SysUser user = userMap.get(otherUserId);
            if (user != null) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("conversationId", conv.getId());
                item.put("userId", user.getId());
                item.put("nickname", user.getNickname() != null ? user.getNickname() : user.getUsername());
                item.put("avatar", user.getAvatar());
                item.put("lastMessage", conv.getLastMessage());
                item.put("lastMessageAt", conv.getLastMessageAt());
                item.put("unreadCount", unreadMap.getOrDefault(conv.getId(), 0));
                result.add(item);
            }
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
        assertConversationParticipant(conv, userId);
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
            if (!shouldShowMessage(msg, userId)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", msg.getId());
            item.put("conversationId", msg.getConversationId());
            item.put("senderId", msg.getSenderId());
            item.put("receiverId", msg.getReceiverId());
            item.put("content", msg.getContent());
            item.put("messageType", msg.getMessageType());
            item.put("photoId", msg.getPhotoId());
            item.put("fileId", msg.getFileId());
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

    private boolean shouldShowMessage(ChatMessage msg, Long userId) {
        if ("image".equals(msg.getMessageType()) && msg.getPhotoId() != null) {
            ChatPhoto photo = chatPhotoRepository.findById(msg.getPhotoId()).orElse(null);
            if (photo == null) {
                return false;
            }
            if (msg.getSenderId().equals(userId)) {
                return !Boolean.TRUE.equals(photo.getIsSenderDelete());
            }
            if (msg.getReceiverId() != null && msg.getReceiverId().equals(userId)) {
                return !Boolean.TRUE.equals(photo.getIsReceiverDelete());
            }
        }

        if ("file".equals(msg.getMessageType()) && msg.getFileId() != null) {
            ChatFile file = chatFileRepository.findById(msg.getFileId()).orElse(null);
            if (file == null) {
                return false;
            }
            if (msg.getSenderId().equals(userId)) {
                return !Boolean.TRUE.equals(file.getIsSenderDelete());
            }
            if (msg.getReceiverId() != null && msg.getReceiverId().equals(userId)) {
                return !Boolean.TRUE.equals(file.getIsReceiverDelete());
            }
        }

        return true;
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

        if (mutualFriendIds.isEmpty()) return result;

        // Batch fetch users
        Map<Long, SysUser> userMap = new LinkedHashMap<>();
        for (SysUser user : userRepository.findByIdIn(mutualFriendIds)) {
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
     * 5. 双方都已清除时，不在请求链路中硬删除；交给定时清理任务统一处理
     */
    @Transactional
    public void clearConversation(Long userId, Long conversationId) {
        ChatConversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("对话不存在"));
        assertConversationParticipant(conv, userId);

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

        log.info("Conversation {} cleared by user {} at {}", conversationId, userId, now);
    }

    /**
     * 定时清理任务使用：硬删除指定对话中所有已软删除的消息、照片和文件
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
        } else if (conv.getUser2Id().equals(userId)) {
            return conv.getClearedAtUser2();
        }
        return null;
    }

    /**
     * 校验当前用户是否属于目标会话，避免越权读取或清空他人聊天。
     */
    private void assertConversationParticipant(ChatConversation conv, Long userId) {
        if (conv == null) {
            throw new ResourceNotFoundException("对话不存在");
        }
        boolean isParticipant = conv.getUser1Id().equals(userId) || conv.getUser2Id().equals(userId);
        if (!isParticipant) {
            throw new PermissionDeniedException("无权访问该对话");
        }
    }

    // ==================== 删除聊天文件/照片（双字段方案） ====================

    /**
     * 删除聊天文件
     * - 如果是发送者删除，设置 isSenderDelete
     * - 如果是接收者删除，设置 isReceiverDelete
     * - 双方都删除后，标记 soft delete，等待定时任务清理
     */
    @Transactional
    public void deleteChatFile(Long fileId, Long userId) {
        ChatFile file = chatFileRepository.findById(fileId).orElse(null);
        if (file == null) {
            log.warn("ChatFile not found: id={}", fileId);
            return;
        }

        ChatMessage message = messageRepository.findByFileId(fileId).orElse(null);
        if (message == null) {
            if (!file.getSenderId().equals(userId)) {
                throw new PermissionDeniedException("无权限删除该文件");
            }
            chatFileRepository.markSoftDeleted(fileId);
            asyncDeleteFileAndRecord(fileId, file.getFilePath());
            log.info("Orphan chat file {} deleted by sender {}", fileId, userId);
            return;
        }

        boolean isSender = file.getSenderId().equals(userId);
        boolean isReceiver = message.getReceiverId() != null && message.getReceiverId().equals(userId);
        if (!isSender && !isReceiver) {
            throw new PermissionDeniedException("无权限删除该文件");
        }

        if (isSender) {
            chatFileRepository.markSenderDeleted(fileId);
            log.info("User {} marked sender_delete for file {}", userId, fileId);
        } else {
            chatFileRepository.markReceiverDeleted(fileId);
            log.info("User {} marked receiver_delete for file {}", userId, fileId);
        }

        boolean senderDeleted = isSender || Boolean.TRUE.equals(file.getIsSenderDelete());
        boolean receiverDeleted = isReceiver || Boolean.TRUE.equals(file.getIsReceiverDelete());
        if (senderDeleted && receiverDeleted) {
            chatFileRepository.markSoftDeleted(fileId);
            asyncDeleteFileAndRecord(fileId, file.getFilePath());
            log.info("File {} marked soft deleted after both parties deleted", fileId);
        }
    }

    /**
     * 删除聊天照片
     * - 如果是发送者删除，设置 isSenderDelete
     * - 如果是接收者删除，设置 isReceiverDelete
     * - 双方都删除后，异步删除物理文件和数据库记录
     */
    @Transactional
    public void deleteChatPhoto(Long photoId, Long userId) {
        ChatPhoto photo = chatPhotoRepository.findById(photoId).orElse(null);
        if (photo == null) {
            log.warn("ChatPhoto not found: id={}", photoId);
            return;
        }

        ChatMessage message = messageRepository.findByPhotoId(photoId).orElse(null);
        if (message == null) {
            if (!photo.getSenderId().equals(userId)) {
                throw new PermissionDeniedException("无权限删除该照片");
            }
            chatPhotoRepository.markSoftDeleted(photoId);
            asyncDeletePhotoAndRecord(photoId, photo.getOriginalPath(), photo.getThumbnailPath());
            log.info("Orphan chat photo {} deleted by sender {}", photoId, userId);
            return;
        }

        boolean isSender = photo.getSenderId().equals(userId);
        boolean isReceiver = message.getReceiverId() != null && message.getReceiverId().equals(userId);
        if (!isSender && !isReceiver) {
            throw new PermissionDeniedException("无权限删除该照片");
        }

        if (isSender) {
            chatPhotoRepository.markSenderDeleted(photoId);
            log.info("User {} marked sender_delete for photo {}", userId, photoId);
        } else {
            chatPhotoRepository.markReceiverDeleted(photoId);
            log.info("User {} marked receiver_delete for photo {}", userId, photoId);
        }

        boolean senderDeleted = isSender || Boolean.TRUE.equals(photo.getIsSenderDelete());
        boolean receiverDeleted = isReceiver || Boolean.TRUE.equals(photo.getIsReceiverDelete());
        if (senderDeleted && receiverDeleted) {
            chatPhotoRepository.markSoftDeleted(photoId);
            asyncDeletePhotoAndRecord(photoId, photo.getOriginalPath(), photo.getThumbnailPath());
            log.info("Photo {} marked soft deleted after both parties deleted", photoId);
        }
    }

    @Async
    public void asyncDeleteFileAndRecord(Long fileId, String filePath) {
        try {
            fileUploadService.deletePhysicalFile(filePath);
            chatFileRepository.hardDeleteById(fileId);
            log.info("Async deleted file record and physical file: id={}, path={}", fileId, filePath);
        } catch (Exception e) {
            log.error("Failed to async delete file: id={}", fileId, e);
        }
    }

    @Async
    public void asyncDeletePhotoAndRecord(Long photoId, String originalPath, String thumbnailPath) {
        try {
            fileUploadService.deletePhysicalFile(originalPath);
            fileUploadService.deletePhysicalFile(thumbnailPath);
            chatPhotoRepository.hardDeleteById(photoId);
            log.info("Async deleted photo record and physical files: id={}, original={}, thumb={}", photoId, originalPath, thumbnailPath);
        } catch (Exception e) {
            log.error("Failed to async delete photo: id={}", photoId, e);
        }
    }

    /**
     * 删除单条文本消息（仅发送者可删除）
     * @param messageId 消息ID
     * @param userId 当前用户ID
     * @throws ResourceNotFoundException 消息不存在
     * @throws PermissionDeniedException 无权限删除
     */
    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        ChatMessage message = messageRepository.findById(messageId).orElse(null);
        if (message == null) {
            throw new ResourceNotFoundException("消息不存在");
        }
        if (!message.getSenderId().equals(userId)) {
            throw new PermissionDeniedException("无权限删除该消息");
        }
        messageRepository.softDeleteById(messageId);
        log.info("Message {} deleted by user {}", messageId, userId);
    }
}
