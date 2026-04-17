package com.pm.userservices.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name = "refresh_tokens",
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_token_hash", columnList = "token")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Refresh_Token {
    @GeneratedValue
    @Id
    private UUID ID;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false, unique = true)
    private String token;
    private String deviceInfo;
    private String ipAddress;
    @Column(nullable = false)
    private Instant expiryAt;


    @Column(nullable = false)
    @Builder.Default
    private Boolean isRevoked= false;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
