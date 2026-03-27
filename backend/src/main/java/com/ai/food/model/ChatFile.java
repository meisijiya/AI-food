package com.ai.food.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Where;
import java.time.LocalDateTime;

@Data
@Entity
@Where(clause = "is_deleted = false")
@Table(name = "chat_file", indexes = {
    @Index(name = "idx_cf_conversation", columnList = "conversation_id"),
    @Index(name = "idx_cf_sender", columnList = "sender_id"),
    @Index(name = "idx_cf_created", columnList = "created_at")
})
public class ChatFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id")
    private Long conversationId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "original_name", length = 255)
    private String originalName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 50)
    private String mimeType;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "is_receiver_delete")
    private Boolean isReceiverDelete = false;

    @Column(name = "is_sender_delete")
    private Boolean isSenderDelete = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
