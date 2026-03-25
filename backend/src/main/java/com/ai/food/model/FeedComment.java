package com.ai.food.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Where;
import java.time.LocalDateTime;

@Data
@Entity
@Where(clause = "is_deleted = false")
@Table(name = "feed_comment", indexes = {
    @Index(name = "idx_fc_post_id", columnList = "post_id"),
    @Index(name = "idx_fc_user_id", columnList = "user_id")
})
public class FeedComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
