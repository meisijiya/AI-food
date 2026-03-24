package com.ai.food.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_follow", indexes = {
    @Index(name = "idx_follower", columnList = "follower_id"),
    @Index(name = "idx_following", columnList = "following_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_follow", columnNames = {"follower_id", "following_id"})
})
public class UserFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "follower_id", nullable = false)
    private Long followerId;

    @Column(name = "following_id", nullable = false)
    private Long followingId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
