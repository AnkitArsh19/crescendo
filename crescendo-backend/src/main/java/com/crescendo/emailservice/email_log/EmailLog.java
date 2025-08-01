package com.crescendo.emailservice.email_log;

import com.crescendo.enums.EmailStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_log")
public class EmailLog {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "userId", nullable = false)
    private UUID userId;

    @Column(name = "appKeyId", nullable = false)
    private UUID appKeyId;

    @Column(name = "toAddress", nullable = false)
    private String toAddress;

    @Column(name = "fromAddress", nullable = false)
    private String fromAddress;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EmailStatus status;

    @Column(name = "providerMessageId", nullable = false)
    private String providerMessageId;

    @Column(name = "error")
    private String error;

    @Column(name = "sentAt", nullable = false)
    private Instant sentAt;

    public EmailLog() {
    }

    public EmailLog(UUID id, UUID userId, UUID appKeyId, String fromAddress, String toAddress, String subject, EmailStatus status, String providerMessageId, String error, Instant sentAt) {
        this.id = id;
        this.userId = userId;
        this.appKeyId = appKeyId;
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.subject = subject;
        this.status = status;
        this.providerMessageId = providerMessageId;
        this.error = error;
        this.sentAt = sentAt;
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

    public UUID getAppKeyId() {
        return appKeyId;
    }

    public void setAppKeyId(UUID appKeyId) {
        this.appKeyId = appKeyId;
    }

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public EmailStatus getStatus() {
        return status;
    }

    public void setStatus(EmailStatus status) {
        this.status = status;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }
}
