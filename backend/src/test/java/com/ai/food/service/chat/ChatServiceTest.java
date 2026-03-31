package com.ai.food.service.chat;

import com.ai.food.model.ChatConversation;
import com.ai.food.model.ChatFile;
import com.ai.food.model.ChatMessage;
import com.ai.food.model.ChatPhoto;
import com.ai.food.exception.PermissionDeniedException;
import com.ai.food.repository.ChatConversationRepository;
import com.ai.food.repository.ChatFileRepository;
import com.ai.food.repository.ChatMessageRepository;
import com.ai.food.repository.ChatPhotoRepository;
import com.ai.food.repository.UserRepository;
import com.ai.food.service.follow.FollowService;
import com.ai.food.service.notification.NotificationService;
import com.ai.food.service.upload.FileUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ChatService soft delete consistency")
class ChatServiceTest {

    @Mock
    private ChatConversationRepository conversationRepository;

    @Mock
    private ChatMessageRepository messageRepository;

    @Mock
    private ChatPhotoRepository chatPhotoRepository;

    @Mock
    private ChatFileRepository chatFileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FollowService followService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private FileUploadService fileUploadService;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
                conversationRepository,
                messageRepository,
                chatPhotoRepository,
                chatFileRepository,
                userRepository,
                followService,
                notificationService,
                stringRedisTemplate,
                fileUploadService
        );

        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get(any(), any())).thenReturn(null);
        when(messageRepository.findLastMessageByConversationId(eq(100L), any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("clearConversation keeps both-cleared cleanup on scheduled job path")
    void clearConversation_doesNotHardDeleteWhenBothUsersAlreadyCleared() {
        ChatConversation initial = new ChatConversation();
        ReflectionTestUtils.setField(initial, "id", 100L);
        ReflectionTestUtils.setField(initial, "user1Id", 1L);
        ReflectionTestUtils.setField(initial, "user2Id", 2L);
        ReflectionTestUtils.setField(initial, "lastMessageAt", LocalDateTime.now().minusMinutes(1));

        ChatConversation bothCleared = new ChatConversation();
        ReflectionTestUtils.setField(bothCleared, "id", 100L);
        ReflectionTestUtils.setField(bothCleared, "user1Id", 1L);
        ReflectionTestUtils.setField(bothCleared, "user2Id", 2L);
        ReflectionTestUtils.setField(bothCleared, "clearedAtUser1", LocalDateTime.now().minusMinutes(2));
        ReflectionTestUtils.setField(bothCleared, "clearedAtUser2", LocalDateTime.now().minusMinutes(1));

        when(conversationRepository.findById(100L))
                .thenReturn(Optional.of(initial), Optional.of(bothCleared));

        chatService.clearConversation(1L, 100L);

        verify(conversationRepository).setClearedAndHiddenAtUser1(eq(100L), any(LocalDateTime.class));
        verify(messageRepository).softDeleteByConversationIdBefore(eq(100L), any(LocalDateTime.class));
        verify(chatPhotoRepository).softDeleteByConversationIdBefore(eq(100L), any(LocalDateTime.class));
        verify(chatFileRepository).softDeleteByConversationIdBefore(eq(100L), any(LocalDateTime.class));
        verify(messageRepository, never()).hardDeleteByConversationId(100L);
        verify(chatPhotoRepository, never()).hardDeleteByConversationId(100L);
        verify(chatFileRepository, never()).hardDeleteByConversationId(100L);
    }

    @Test
    @DisplayName("非会话参与者不能读取聊天历史")
    void getChatHistory_deniesNonParticipant() {
        ChatConversation conv = new ChatConversation();
        ReflectionTestUtils.setField(conv, "id", 200L);
        ReflectionTestUtils.setField(conv, "user1Id", 1L);
        ReflectionTestUtils.setField(conv, "user2Id", 2L);
        when(conversationRepository.findById(200L)).thenReturn(Optional.of(conv));

        assertThrows(PermissionDeniedException.class, () -> chatService.getChatHistory(200L, 3L, 0, 20));
    }

    @Test
    @DisplayName("非会话参与者不能清空聊天")
    void clearConversation_deniesNonParticipant() {
        ChatConversation conv = new ChatConversation();
        ReflectionTestUtils.setField(conv, "id", 300L);
        ReflectionTestUtils.setField(conv, "user1Id", 1L);
        ReflectionTestUtils.setField(conv, "user2Id", 2L);
        when(conversationRepository.findById(300L)).thenReturn(Optional.of(conv));

        assertThrows(PermissionDeniedException.class, () -> chatService.clearConversation(4L, 300L));
    }

    @Test
    @DisplayName("双方删除照片后会触发底层物理删除")
    void deleteChatPhoto_triggersPhysicalCleanupWhenBothDeleted() {
        ChatPhoto photo = new ChatPhoto();
        ReflectionTestUtils.setField(photo, "id", 10L);
        ReflectionTestUtils.setField(photo, "senderId", 1L);
        ReflectionTestUtils.setField(photo, "originalPath", "/uploads/chat-photos/20260331/a.jpg");
        ReflectionTestUtils.setField(photo, "thumbnailPath", "/uploads/chat-photos/20260331/a_thumb.jpg");
        ReflectionTestUtils.setField(photo, "isReceiverDelete", true);
        ChatMessage message = new ChatMessage();
        ReflectionTestUtils.setField(message, "receiverId", 2L);
        when(chatPhotoRepository.findById(10L)).thenReturn(Optional.of(photo));
        when(messageRepository.findByPhotoId(10L)).thenReturn(Optional.of(message));
        doNothing().when(chatPhotoRepository).markSenderDeleted(10L);
        doNothing().when(chatPhotoRepository).markSoftDeleted(10L);
        doNothing().when(chatPhotoRepository).hardDeleteById(10L);

        chatService.deleteChatPhoto(10L, 1L);

        verify(chatPhotoRepository).markSoftDeleted(10L);
        verify(fileUploadService).deletePhysicalFile("/uploads/chat-photos/20260331/a.jpg");
        verify(fileUploadService).deletePhysicalFile("/uploads/chat-photos/20260331/a_thumb.jpg");
        verify(chatPhotoRepository).hardDeleteById(10L);
    }

    @Test
    @DisplayName("无消息记录的孤儿文件允许发送者本人删除")
    void deleteChatFile_allowsSenderToDeleteOrphanUpload() {
        ChatFile file = new ChatFile();
        ReflectionTestUtils.setField(file, "id", 11L);
        ReflectionTestUtils.setField(file, "senderId", 1L);
        ReflectionTestUtils.setField(file, "filePath", "/uploads/chat-files/20260331/demo.pdf");
        when(chatFileRepository.findById(11L)).thenReturn(Optional.of(file));
        when(messageRepository.findByFileId(11L)).thenReturn(Optional.empty());
        doNothing().when(chatFileRepository).markSoftDeleted(11L);
        doNothing().when(chatFileRepository).hardDeleteById(11L);

        chatService.deleteChatFile(11L, 1L);

        verify(chatFileRepository).markSoftDeleted(11L);
        verify(fileUploadService).deletePhysicalFile("/uploads/chat-files/20260331/demo.pdf");
        verify(chatFileRepository).hardDeleteById(11L);
    }
}
