package com.ai.food.service.chat;

import com.ai.food.common.model.ChatConversation;
import com.ai.food.common.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 聊天服务 facade：保留原 752 行 {@code ChatService} 的全部公开方法签名，内部按职责 delegate 到
 * {@link ChatMessageService} / {@link ChatUnreadService} / {@link ChatPermissionService}。
 * <p>
 * Controller / {@code ChatWebSocketHandler} / {@code CleanupSoftDeletedJob} 等调用方无需修改，
 * 仍按原 {@code chatService.xxx(...)} 调用。
 * </p>
 *
 * <p>ponytail: 纯转调，0 业务逻辑；事务边界由各子 service 自己负责。{@link Async} 注解保留在 facade
 * 上以便 Spring 异步代理仍能命中入口（内部 self-invocation 不经过 AOP 代理）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageService chatMessageService;
    private final ChatUnreadService chatUnreadService;
    private final ChatPermissionService chatPermissionService;

    // ===== 权限（delegate → ChatPermissionService） =====

    public String checkSendPermission(Long senderId, Long receiverId) {
        return chatPermissionService.checkSendPermission(senderId, receiverId);
    }

    public int getRemainingMessages(Long senderId, Long receiverId) {
        return chatPermissionService.getRemainingMessages(senderId, receiverId);
    }

    // ===== 消息 CRUD + 文件/照片删除（delegate → ChatMessageService） =====

    public ChatMessage sendMessage(Long senderId, Long receiverId, String content, String messageType, Long photoId, Long fileId) {
        return chatMessageService.sendMessage(senderId, receiverId, content, messageType, photoId, fileId);
    }

    public ChatConversation getOrCreateConversation(Long userId1, Long userId2) {
        return chatMessageService.getOrCreateConversation(userId1, userId2);
    }

    public Map<String, Object> getOrCreateConversationWith(Long userId, Long otherUserId) {
        return chatMessageService.getOrCreateConversationWith(userId, otherUserId);
    }

    public List<Map<String, Object>> getConversationList(Long userId) {
        return chatMessageService.getConversationList(userId);
    }

    public Map<String, Object> getChatHistory(Long conversationId, Long userId, int page, int size) {
        return chatMessageService.getChatHistory(conversationId, userId, page, size);
    }

    public void markAsRead(Long userId, Long conversationId) {
        chatMessageService.markAsRead(userId, conversationId);
    }

    public void clearConversation(Long userId, Long conversationId) {
        chatMessageService.clearConversation(userId, conversationId);
    }

    public void hardDeleteClearedMessages(Long conversationId) {
        chatMessageService.hardDeleteClearedMessages(conversationId);
    }

    public void deleteChatFile(Long fileId, Long userId) {
        chatMessageService.deleteChatFile(fileId, userId);
    }

    public void deleteChatPhoto(Long photoId, Long userId) {
        chatMessageService.deleteChatPhoto(photoId, userId);
    }

    /**
     * 异步删除物理文件并硬删除数据库记录。
     * <p>ponytail: {@link Async} 注解必须保留在 facade 上，{@code this.asyncXxx(...)} 不经过 Spring 代理。</p>
     */
    @Async
    public void asyncDeleteFileAndRecord(Long fileId, String filePath) {
        chatMessageService.asyncDeleteFileAndRecord(fileId, filePath);
    }

    /**
     * 异步删除原图 + 缩略图并硬删除数据库记录。
     */
    @Async
    public void asyncDeletePhotoAndRecord(Long photoId, String originalPath, String thumbnailPath) {
        chatMessageService.asyncDeletePhotoAndRecord(photoId, originalPath, thumbnailPath);
    }

    public void deleteMessage(Long messageId, Long userId) {
        chatMessageService.deleteMessage(messageId, userId);
    }

    // ===== 未读 + 在线（delegate → ChatUnreadService） =====

    public Map<String, Object> getUnreadCounts(Long userId) {
        return chatUnreadService.getUnreadCounts(userId);
    }

    public int getTotalUnreadCount(Long userId) {
        return chatUnreadService.getTotalUnreadCount(userId);
    }

    public List<Map<String, Object>> getContacts(Long userId) {
        return chatUnreadService.getContacts(userId);
    }

    public void setUserOnline(Long userId) {
        chatUnreadService.setUserOnline(userId);
    }

    public void setUserOffline(Long userId) {
        chatUnreadService.setUserOffline(userId);
    }

    public boolean isOnline(Long userId) {
        return chatUnreadService.isOnline(userId);
    }
}
