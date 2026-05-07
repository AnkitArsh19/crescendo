package com.crescendo.emailservice.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Raised when a user revokes an API key.
 */
public class ApiKeyRevokedEvent extends BaseDomainEvent {

    private final UUID userId;

    public ApiKeyRevokedEvent(UUID apiKeyId, UUID userId) {
        super(apiKeyId);
        this.userId = userId;
    }

    public UUID getUserId() { return userId; }
}
