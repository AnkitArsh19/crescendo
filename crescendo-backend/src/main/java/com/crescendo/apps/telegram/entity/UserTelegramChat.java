package com.crescendo.apps.telegram.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_telegram_chat",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_chat", columnNames = {"userId", "chatId"})
        },
        indexes = {
                @Index(name = "idx_user_tg_chat_user", columnList = "userId"),
                @Index(name = "idx_user_tg_chat_tguser", columnList = "telegramUserId"),
                @Index(name = "idx_user_tg_chat_chatid", columnList = "chatId")
        })
public class UserTelegramChat {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "userId", nullable = false)
    private UUID userId;

    @Column(name = "telegramUserId", nullable = false)
    private Long telegramUserId;

    @Column(name = "chatId", nullable = false, length = 64)
    private String chatId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "type", nullable = false, length = 32)
    private String type; // private, group, supergroup, channel

    @Column(name = "botStatus", length = 32)
    private String botStatus; // administrator, member, left, kicked

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updatedAt", nullable = false)
    private Instant updatedAt;

    public UserTelegramChat() {}

    public UserTelegramChat(UUID id, UUID userId, Long telegramUserId, String chatId, String title, String type, String botStatus) {
        this.id = id;
        this.userId = userId;
        this.telegramUserId = telegramUserId;
        this.chatId = chatId;
        this.title = title;
        this.type = type;
        this.botStatus = botStatus;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Long getTelegramUserId() {
        return telegramUserId;
    }

    public void setTelegramUserId(Long telegramUserId) {
        this.telegramUserId = telegramUserId;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public String getChatType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBotStatus() {
        return botStatus;
    }

    public void setBotStatus(String botStatus) {
        this.botStatus = botStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
