package com.ai.food.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "chat_conversation", indexes = {
    @Index(name = "idx_user1", columnList = "user1_id, last_message_at"),
    @Index(name = "idx_user2", columnList = "user2_id, last_message_at")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_conversation_key", columnNames = {"conversation_key"})
})
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_key", nullable = false, length = 128)
    private String conversationKey;

    @Column(name = "user1_id", nullable = false)
    private Long user1Id;

    @Column(name = "user2_id", nullable = false)
    private Long user2Id;

    @Column(name = "last_message", columnDefinition = "TEXT")
    private String lastMessage;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastMessageAt = LocalDateTime.now();
    }

    public static String generateKey(Long userId1, Long userId2) {
        long min = Math.min(userId1, userId2);
        long max = Math.max(userId1, userId2);
        return min + "_" + max;
    }
}
