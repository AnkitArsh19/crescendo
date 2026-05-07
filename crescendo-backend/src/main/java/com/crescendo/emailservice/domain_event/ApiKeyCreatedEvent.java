package com.crescendo.emailservice.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Raised when a user creates a new API key for the email service.
 */
public class ApiKeyCreatedEvent extends BaseDomainEvent {

    private final UUID userId;
    private final String keyPrefix;

    public ApiKeyCreatedEvent(UUID apiKeyId, UUID userId, String keyPrefix) {
        super(apiKeyId);
        this.userId = userId;
        this.keyPrefix = keyPrefix;
    }

    public UUID getUserId() { return userId; }
    public String getKeyPrefix() { return keyPrefix; }
}
