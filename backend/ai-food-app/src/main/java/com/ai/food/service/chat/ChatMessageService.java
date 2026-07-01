package com.ai.food.service.chat;

import com.ai.food.common.mapper.ChatConversationMapper;
import com.ai.food.common.mapper.ChatFileMapper;
import com.ai.food.common.mapper.ChatMessageMapper;
import com.ai.food.common.mapper.ChatPhotoMapper;
import com.ai.food.common.mapper.UserMapper;
import com.ai.food.common.model.ChatConversation;
import com.ai.food.common.model.ChatFile;
import com.ai.food.common.model.ChatMessage;
import com.ai.food.common.model.ChatPhoto;
import com.ai.food.common.model.SysUser;
import com.ai.food.exception.BusinessException;
import com.ai.food.exception.PermissionDeniedException;
import com.ai.food.exception.ResourceNotFoundException;
import com.ai.food.service.follow.FollowService;
import com.ai.food.service.notification.NotificationService;
import com.ai.food.service.upload.FileUploadService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.ai.food.service.chat.ChatUtil.MAX_NON_MUTUAL_MESSAGES;

/**
 * 聊天消息服务（消息 CRUD + 文件照片删除）。
 * <p>
 * 提取自原 {@code ChatService}（752 行），按 fix-7 FeedService 拆分同模式：是聊天模块的"核心 service"，
 * 其它副作用（未读、权限）通过 {@link ChatUnreadService} / {@link ChatPermissionService} 注入调用。
 * </p>
 * <p>
 * 继承 {@link ServiceImpl} 后 {@code baseMapper} 指向 {@link ChatMessageMapper}；其他实体的 CRUD
 * 走注入的 Mapper 字段。
 * </p>
 *
 * <p>ponytail: 不引入新抽象；{@code @Async} 物理文件删除保留在 facade 公开方法签名上以便异步代理生效。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService extends ServiceImpl<ChatMessageMapper, ChatMessage> {

    private final ChatConversationMapper conversationMapper;
    private final ChatPhotoMapper chatPhotoMapper;
    private final ChatFileMapper chatFileMapper;
    private final UserMapper userMapper;
    private final FollowService followService;
    private final NotificationService notificationService;
    private final FileUploadService fileUploadService;
    private final ChatUnreadService chatUnreadService;
    private final ChatPermissionService chatPermissionService;

    /**
     * 发送消息：写入消息记录、刷新会话预览、推送未读与通知、累计非互关注计数。
     */
    @Transactional
    public ChatMessage sendMessage(Long senderId, Long receiverId, String content, String messageType, Long photoId, Long fileId) {
        String permission = chatPermissionService.checkSendPermission(senderId, receiverId);
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
        // MP insert 会自动回填主键 + MetaObjectHandler 写入 createdAt/updatedAt
        baseMapper.insert(message);

        // 更新 chat_photo 和 chat_file 的 conversationId
        if (photoId != null) {
            chatPhotoMapper.updateConversationId(photoId, conversation.getId());
        }
        if (fileId != null) {
            chatFileMapper.updateConversationId(fileId, conversation.getId());
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
        // getOrCreateConversation 已经 insert 过；此处必然有 id
        conversationMapper.updateById(conversation);

        chatUnreadService.incrementUnread(receiverId, conversation.getId());

        String senderName = "匿名用户";
        String senderAvatar = null;
        SysUser sender = userMapper.selectById(senderId);
        if (sender != null) {
            senderName = sender.getNickname() != null ? sender.getNickname() : sender.getUsername();
            senderAvatar = sender.getAvatar();
        }
        notificationService.updateChatNotification(receiverId, conversation.getId(),
                senderId, senderName, senderAvatar, content);

        if (!followService.isMutualFollow(senderId, receiverId)) {
            chatPermissionService.incrementMessageCount(senderId, receiverId);
        }

        log.info("Message sent: from={} to={}, conversationId={}", senderId, receiverId, conversation.getId());
        return message;
    }

    /**
     * 重置接收方的 hiddenAt，使会话在收到新消息后重新出现在聊天列表
     * 注意：clearedAt 保持不变，确保旧消息继续被过滤不可见
     */
    private void resetHiddenForReceiver(ChatConversation conversation, Long receiverId) {
        if (conversation.getUser1Id().equals(receiverId) && conversation.getHiddenAtUser1() != null) {
            conversationMapper.resetHiddenAtUser1(conversation.getId());
            conversation.setHiddenAtUser1(null);
        } else if (conversation.getUser2Id().equals(receiverId) && conversation.getHiddenAtUser2() != null) {
            conversationMapper.resetHiddenAtUser2(conversation.getId());
            conversation.setHiddenAtUser2(null);
        }
    }

    /**
     * 获取或创建两个用户之间的会话。
     */
    public ChatConversation getOrCreateConversation(Long userId1, Long userId2) {
        String key = ChatConversation.generateKey(userId1, userId2);
        ChatConversation existing = conversationMapper.findByConversationKey(key);
        if (existing != null) {
            return existing;
        }
        ChatConversation conversation = new ChatConversation();
        conversation.setConversationKey(key);
        conversation.setUser1Id(Math.min(userId1, userId2));
        conversation.setUser2Id(Math.max(userId1, userId2));
        conversationMapper.insert(conversation);
        return conversation;
    }

    /**
     * 获取或创建与指定用户的对话，返回包含 conversationId 的 Map
     */
    public Map<String, Object> getOrCreateConversationWith(Long userId, Long otherUserId) {
        ChatConversation conv = getOrCreateConversation(userId, otherUserId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conversationId", conv.getId());
        result.put("userId", otherUserId);

        SysUser user = userMapper.selectById(otherUserId);
        if (user != null) {
            result.put("nickname", user.getNickname() != null ? user.getNickname() : user.getUsername());
            result.put("avatar", user.getAvatar());
        }

        return result;
    }

    /**
     * 获取用户的会话列表（按最近消息时间倒序）。
     */
    public List<Map<String, Object>> getConversationList(Long userId) {
        List<ChatConversation> conversations = conversationMapper.findByUserIdOrderByLastMessageAtDesc(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        Map<Long, Integer> unreadMap = chatUnreadService.getUnreadMap(userId);

        // Collect all otherUserIds and batch fetch
        List<Long> otherUserIds = new ArrayList<>();
        for (ChatConversation conv : conversations) {
            otherUserIds.add(conv.getUser1Id().equals(userId) ? conv.getUser2Id() : conv.getUser1Id());
        }
        Map<Long, SysUser> userMap = new LinkedHashMap<>();
        for (SysUser user : userMapper.findByIdIn(otherUserIds)) {
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
     * - mapper SQL 显式过滤 is_deleted = 0，模拟原 @Where 注解语义
     */
    @Transactional
    public Map<String, Object> getChatHistory(Long conversationId, Long userId, int page, int size) {
        ChatConversation conv = conversationMapper.selectById(conversationId);
        assertConversationParticipant(conv, userId);
        LocalDateTime clearedAt = getClearedAtForUser(conv, userId);

        // MP 分页 1-based，控制器传 0-based 这里 +1
        Page<ChatMessage> messagePage = new Page<>(page + 1, size);

        if (clearedAt != null) {
            // 只返回清除时间点之后的消息（clearedAt 永不重置）
            baseMapper.findByConversationIdAfterOrderByCreatedAtDesc(conversationId, clearedAt, messagePage);
        } else {
            baseMapper.findByConversationIdOrderByCreatedAtDesc(conversationId, messagePage);
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (ChatMessage msg : messagePage.getRecords()) {
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
        result.put("page", (int) messagePage.getCurrent() - 1);
        result.put("size", messagePage.getSize());
        result.put("totalElements", messagePage.getTotal());
        result.put("totalPages", (int) messagePage.getPages());
        return result;
    }

    /**
     * 根据消息的 photoId/fileId 关联判断对当前用户是否可见（双方删除标记任一为真即隐藏）。
     */
    private boolean shouldShowMessage(ChatMessage msg, Long userId) {
        if ("image".equals(msg.getMessageType()) && msg.getPhotoId() != null) {
            ChatPhoto photo = chatPhotoMapper.selectById(msg.getPhotoId());
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
            ChatFile file = chatFileMapper.selectById(msg.getFileId());
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

    /**
     * 将某会话中接收方未读消息标记为已读，并同步递减 Redis / 通知中心未读计数。
     */
    @Transactional
    public void markAsRead(Long userId, Long conversationId) {
        int updated = baseMapper.markAsRead(conversationId, userId);
        if (updated > 0) {
            chatUnreadService.clearUnread(userId, conversationId);

            // 同步递减全局通知未读计数
            notificationService.decrementUnread(userId, updated);

            // 同步更新通知中心的聊天未读计数
            notificationService.decrementChatNotificationUnread(userId, conversationId, updated);

            log.info("Marked {} messages as read: userId={}, conversationId={}", updated, userId, conversationId);
        }
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
        ChatConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new ResourceNotFoundException("对话不存在");
        }
        assertConversationParticipant(conv, userId);

        LocalDateTime now = LocalDateTime.now();

        // 同时设置 clearedAt 和 hiddenAt
        if (conv.getUser1Id().equals(userId)) {
            conversationMapper.setClearedAndHiddenAtUser1(conversationId, now);
        } else {
            conversationMapper.setClearedAndHiddenAtUser2(conversationId, now);
        }

        // 软删除清除时间点之前的消息（mapper SQL 显式过滤 is_deleted = 0）
        baseMapper.softDeleteByConversationIdBefore(conversationId, now);
        chatPhotoMapper.softDeleteByConversationIdBefore(conversationId, now);
        chatFileMapper.softDeleteByConversationIdBefore(conversationId, now);

        // 清理 Redis 未读计数
        chatUnreadService.clearUnread(userId, conversationId);

        // 更新 lastMessage（清除后应为空或仅显示新消息）
        Page<ChatMessage> lastMsgPage = new Page<>(1, 1);
        List<ChatMessage> remaining = baseMapper.findLastMessageByConversationId(conversationId, lastMsgPage);
        if (remaining.isEmpty()) {
            conversationMapper.updateLastMessage(conversationId, null, conv.getLastMessageAt());
        } else {
            ChatMessage last = remaining.get(0);
            String preview = last.getContent().length() > 50 ? last.getContent().substring(0, 50) + "..." : last.getContent();
            conversationMapper.updateLastMessage(conversationId, preview, last.getCreatedAt());
        }

        log.info("Conversation {} cleared by user {} at {}", conversationId, userId, now);
    }

    /**
     * 定时清理任务使用：硬删除指定对话中所有已软删除的消息、照片和文件
     */
    @Transactional
    public void hardDeleteClearedMessages(Long conversationId) {
        baseMapper.hardDeleteByConversationId(conversationId);
        chatPhotoMapper.hardDeleteByConversationId(conversationId);
        chatFileMapper.hardDeleteByConversationId(conversationId);
    }

    /**
     * 获取某用户在某会话上的 clearedAt，用于聊天历史过滤。
     */
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
        ChatFile file = chatFileMapper.selectById(fileId);
        if (file == null) {
            log.warn("ChatFile not found: id={}", fileId);
            return;
        }

        ChatMessage message = baseMapper.findByFileId(fileId);
        if (message == null) {
            if (!file.getSenderId().equals(userId)) {
                throw new PermissionDeniedException("无权限删除该文件");
            }
            chatFileMapper.markSoftDeleted(fileId);
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
            chatFileMapper.markSenderDeleted(fileId);
            log.info("User {} marked sender_delete for file {}", userId, fileId);
        } else {
            chatFileMapper.markReceiverDeleted(fileId);
            log.info("User {} marked receiver_delete for file {}", userId, fileId);
        }

        boolean senderDeleted = isSender || Boolean.TRUE.equals(file.getIsSenderDelete());
        boolean receiverDeleted = isReceiver || Boolean.TRUE.equals(file.getIsReceiverDelete());
        if (senderDeleted && receiverDeleted) {
            chatFileMapper.markSoftDeleted(fileId);
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
        ChatPhoto photo = chatPhotoMapper.selectById(photoId);
        if (photo == null) {
            log.warn("ChatPhoto not found: id={}", photoId);
            return;
        }

        ChatMessage message = baseMapper.findByPhotoId(photoId);
        if (message == null) {
            if (!photo.getSenderId().equals(userId)) {
                throw new PermissionDeniedException("无权限删除该照片");
            }
            chatPhotoMapper.markSoftDeleted(photoId);
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
            chatPhotoMapper.markSenderDeleted(photoId);
            log.info("User {} marked sender_delete for photo {}", userId, photoId);
        } else {
            chatPhotoMapper.markReceiverDeleted(photoId);
            log.info("User {} marked receiver_delete for photo {}", userId, photoId);
        }

        boolean senderDeleted = isSender || Boolean.TRUE.equals(photo.getIsSenderDelete());
        boolean receiverDeleted = isReceiver || Boolean.TRUE.equals(photo.getIsReceiverDelete());
        if (senderDeleted && receiverDeleted) {
            chatPhotoMapper.markSoftDeleted(photoId);
            asyncDeletePhotoAndRecord(photoId, photo.getOriginalPath(), photo.getThumbnailPath());
            log.info("Photo {} marked soft deleted after both parties deleted", photoId);
        }
    }

    /**
     * 异步删除物理文件并硬删除数据库记录。
     */
    @Async
    public void asyncDeleteFileAndRecord(Long fileId, String filePath) {
        try {
            fileUploadService.deletePhysicalFile(filePath);
            chatFileMapper.hardDeleteById(fileId);
            log.info("Async deleted file record and physical file: id={}, path={}", fileId, filePath);
        } catch (Exception e) {
            log.error("Failed to async delete file: id={}", fileId, e);
        }
    }

    /**
     * 异步删除原图 + 缩略图并硬删除数据库记录。
     */
    @Async
    public void asyncDeletePhotoAndRecord(Long photoId, String originalPath, String thumbnailPath) {
        try {
            fileUploadService.deletePhysicalFile(originalPath);
            fileUploadService.deletePhysicalFile(thumbnailPath);
            chatPhotoMapper.hardDeleteById(photoId);
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
        ChatMessage message = baseMapper.selectById(messageId);
        if (message == null) {
            throw new ResourceNotFoundException("消息不存在");
        }
        if (!message.getSenderId().equals(userId)) {
            throw new PermissionDeniedException("无权限删除该消息");
        }
        baseMapper.softDeleteById(messageId);
        log.info("Message {} deleted by user {}", messageId, userId);
    }

    // ponytail: MSG_COUNT_KEY 的自增已下沉到 ChatPermissionService#incrementMessageCount，
    // 本 service 不再直接持有 StringRedisTemplate（避免字段语义错位）。
}
