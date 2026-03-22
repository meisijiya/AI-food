package com.ai.food.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Where;
import java.time.LocalDateTime;

@Data
@Entity
@Where(clause = "is_deleted = false")
@Table(name = "conversation_session")
public class ConversationSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "session_id", unique = true, nullable = false, length = 64)
    private String sessionId;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "total_questions")
    private Integer totalQuestions;
    
    @Column(name = "current_question_count")
    private Integer currentQuestionCount = 0;
    
    @Column(name = "interrupt_count")
    private Integer interruptCount = 0;
    
    @Column(length = 20)
    private String mode; // inertia/random
    
    @Column(length = 20)
    private String status = "active";
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}