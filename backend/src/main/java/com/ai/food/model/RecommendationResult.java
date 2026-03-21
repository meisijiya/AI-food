package com.ai.food.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
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
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}