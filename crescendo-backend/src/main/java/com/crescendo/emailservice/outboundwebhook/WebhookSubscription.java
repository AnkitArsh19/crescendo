package com.crescendo.emailservice.outboundwebhook;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "webhook_subscriptions",
    indexes = {
        @Index(name = "idx_webhook_subscription_user", columnList = "userId")
    })
public class WebhookSubscription {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "userId", nullable = false)
    private UUID userId;

    @Column(name = "url", nullable = false, length = 1024)
    private String url;

    @Column(name = "signingSecret", nullable = false, length = 128)
    private String signingSecret;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "subscribedEvents", columnDefinition = "jsonb")
    private Set<String> subscribedEvents;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    public WebhookSubscription() {}

    public WebhookSubscription(UUID id, UUID userId, String url, String signingSecret, Set<String> subscribedEvents) {
        this.id = id;
        this.userId = userId;
        this.url = url;
        this.signingSecret = signingSecret;
        this.subscribedEvents = subscribedEvents;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getSigningSecret() { return signingSecret; }
    public Set<String> getSubscribedEvents() { return subscribedEvents; }
    public void setSubscribedEvents(Set<String> subscribedEvents) { this.subscribedEvents = subscribedEvents; }
    public Instant getCreatedAt() { return createdAt; }
}
