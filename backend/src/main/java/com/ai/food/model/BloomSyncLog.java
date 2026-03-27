package com.ai.food.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "bloom_sync_log")
public class BloomSyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "sync_type", length = 20)
    private String syncType;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "error_msg", columnDefinition = "TEXT")
    private String errorMsg;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    @PrePersist
    protected void onCreate() {
        syncedAt = LocalDateTime.now();
    }
}