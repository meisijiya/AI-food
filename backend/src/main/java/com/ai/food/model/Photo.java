package com.ai.food.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Where;
import java.time.LocalDateTime;

@Data
@Entity
@Where(clause = "is_deleted = false")
@Table(name = "photo", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_session", columnList = "related_session_id")
})
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "original_path", nullable = false, length = 500)
    private String originalPath;

    @Column(name = "thumbnail_path", length = 500)
    private String thumbnailPath;

    @Column(name = "related_session_id", length = 64)
    private String relatedSessionId;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "original_size")
    private Long originalSize;

    @Column(name = "thumbnail_size")
    private Long thumbnailSize;

    @Column(name = "mime_type", length = 50)
    private String mimeType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
