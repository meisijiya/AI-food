package com.ai.food.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Where;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Where(clause = "is_deleted = false")
@Table(name = "recommendation_result", indexes = {
    @Index(name = "idx_session", columnList = "session_id")
})
public class RecommendationResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;
    
    @Column(length = 20)
    private String mode;
    
    @Column(name = "food_name", length = 100)
    private String foodName;
    
    @Column(name = "old_food", length = 100)
    private String oldFood; // 随机模式的旧值
    
    @Column(name = "similarity_score", precision = 3, scale = 2)
    private BigDecimal similarityScore;
    
    @Column(columnDefinition = "TEXT")
    private String reason;
    
    @Column(name = "photo_url", length = 500)
    private String photoUrl;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}