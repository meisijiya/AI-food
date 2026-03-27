package com.ai.food.service.chat;

import com.ai.food.model.ChatConversation;
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
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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
}
