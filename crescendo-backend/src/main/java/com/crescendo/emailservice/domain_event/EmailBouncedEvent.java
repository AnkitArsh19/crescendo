package com.crescendo.emailservice.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Raised when an email bounces back from the recipient's mail server.
 */
public class EmailBouncedEvent extends BaseDomainEvent {

    private final UUID domainId;
    private final String bounceReason;

    public EmailBouncedEvent(UUID emailId, UUID domainId, String bounceReason) {
        super(emailId);
        this.domainId = domainId;
        this.bounceReason = bounceReason;
    }

    public UUID getDomainId() { return domainId; }
    public String getBounceReason() { return bounceReason; }
}
