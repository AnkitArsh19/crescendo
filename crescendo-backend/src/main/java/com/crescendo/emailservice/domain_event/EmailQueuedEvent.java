package com.crescendo.emailservice.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Raised when an email is queued for sending via the email pipeline.
 */
public class EmailQueuedEvent extends BaseDomainEvent {

    private final UUID userId;
    private final String toAddress;

    public EmailQueuedEvent(UUID emailId, UUID userId, String toAddress) {
        super(emailId);
        this.userId = userId;
        this.toAddress = toAddress;
    }

    public UUID getUserId() { return userId; }
    public String getToAddress() { return toAddress; }
}
