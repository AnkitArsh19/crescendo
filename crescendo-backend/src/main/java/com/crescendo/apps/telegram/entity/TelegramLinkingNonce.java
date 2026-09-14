package com.crescendo.apps.telegram.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "telegram_linking_nonce", indexes = {
        @Index(name = "idx_tg_nonce_token", columnList = "token", unique = true),
        @Index(name = "idx_tg_nonce_user", columnList = "userId"),
        @Index(name = "idx_tg_nonce_status", columnList = "status")
})
public class TelegramLinkingNonce {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "token", nullable = false, length = 64, unique = true)
    private String token;

    @Column(name = "userId", nullable = false)
    private UUID userId;

    @Column(name = "expiresAt", nullable = false)
    private Instant expiresAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // PENDING, COMPLETED, EXPIRED

    @Column(name = "telegramUserId")
    private Long telegramUserId;

    @Column(name = "telegramUsername", length = 100)
    private String telegramUsername;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completedAt")
    private Instant completedAt;

    public TelegramLinkingNonce() {}

    public TelegramLinkingNonce(UUID id, String token, UUID userId, Instant expiresAt) {
        this.id = id;
        this.token = token;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.status = "PENDING";
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getTelegramUserId() {
        return telegramUserId;
    }

    public void setTelegramUserId(Long telegramUserId) {
        this.telegramUserId = telegramUserId;
    }

    public String getTelegramUsername() {
        return telegramUsername;
    }

    public void setTelegramUsername(String telegramUsername) {
        this.telegramUsername = telegramUsername;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
