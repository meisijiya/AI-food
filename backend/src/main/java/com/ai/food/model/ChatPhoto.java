package com.ai.food.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Where;
import java.time.LocalDateTime;

@Data
@Entity
@Where(clause = "is_deleted = false")
@Table(name = "chat_photo", indexes = {
    @Index(name = "idx_cp_conversation", columnList = "conversation_id"),
    @Index(name = "idx_cp_sender", columnList = "sender_id"),
    @Index(name = "idx_cp_created", columnList = "created_at")
})
public class ChatPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "original_path", nullable = false, length = 500)
    private String originalPath;

    @Column(name = "thumbnail_path", length = 500)
    private String thumbnailPath;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "original_size")
    private Long originalSize;

    @Column(name = "thumbnail_size")
    private Long thumbnailSize;

    @Column(name = "mime_type", length = 50)
    private String mimeType;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
