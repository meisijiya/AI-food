package com.ai.food.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "qa_record", indexes = {
    @Index(name = "idx_session", columnList = "session_id")
})
public class QaRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;
    
    @Column(name = "question_type", length = 20)
    private String questionType; // question/chat/2question/interrupt
    
    @Column(name = "param_name", length = 50)
    private String paramName;
    
    @Column(name = "ai_question", columnDefinition = "TEXT")
    private String aiQuestion;
    
    @Column(name = "user_answer", columnDefinition = "TEXT")
    private String userAnswer;
    
    @Column(name = "is_valid")
    private Boolean isValid = true;
    
    @Column(name = "question_order")
    private Integer questionOrder;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}