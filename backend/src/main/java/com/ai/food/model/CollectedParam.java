package com.ai.food.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "collected_params", uniqueConstraints = {
    @UniqueConstraint(name = "uk_session_param", columnNames = {"session_id", "param_name"})
})
public class CollectedParam {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;
    
    @Column(name = "param_name", length = 50)
    private String paramName;
    
    @Column(name = "param_value", columnDefinition = "TEXT")
    private String paramValue;
    
    @Column(name = "param_type", length = 20)
    private String paramType; // required/optional
    
    @Column(name = "collected_at")
    private LocalDateTime collectedAt;
    
    @PrePersist
    protected void onCreate() {
        collectedAt = LocalDateTime.now();
    }
}