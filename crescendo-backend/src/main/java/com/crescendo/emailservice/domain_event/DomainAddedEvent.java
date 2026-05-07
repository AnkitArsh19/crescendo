package com.crescendo.emailservice.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Raised when a user adds a new domain for email sending.
 */
public class DomainAddedEvent extends BaseDomainEvent {

    private final UUID userId;
    private final String domainName;

    public DomainAddedEvent(UUID domainId, UUID userId, String domainName) {
        super(domainId);
        this.userId = userId;
        this.domainName = domainName;
    }

    public UUID getUserId() { return userId; }
    public String getDomainName() { return domainName; }
}
