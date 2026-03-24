package com.ai.food.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Where;
import java.time.LocalDateTime;

@Data
@Entity
@Where(clause = "is_deleted = false")
@Table(name = "feed_post", indexes = {
    @Index(name = "idx_feed_user_id", columnList = "user_id"),
    @Index(name = "idx_feed_session_id", columnList = "session_id"),
    @Index(name = "idx_feed_published_at", columnList = "published_at"),
    @Index(name = "idx_feed_food_name", columnList = "food_name")
})
public class FeedPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "food_name", length = 100)
    private String foodName;

    @Column(name = "comment_preview", length = 100)
    private String commentPreview;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "original_photo_url", length = 500)
    private String originalPhotoUrl;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "collected_params", columnDefinition = "JSON")
    private String collectedParams;

    @Column(name = "like_count")
    private Integer likeCount = 0;

    @Column(name = "comment_count")
    private Integer commentCount = 0;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @PrePersist
    protected void onCreate() {
        publishedAt = LocalDateTime.now();
    }
}
