package com.ai.food.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Where;
import java.time.LocalDateTime;

@Data
@Entity
@Where(clause = "is_deleted = false")
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

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;
    
    @PrePersist
    protected void onCreate() {
        collectedAt = LocalDateTime.now();
    }
}