package com.crescendo.emailservice.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Raised when an email bounces back from the recipient's mail server.
 */
public class EmailBouncedEvent extends BaseDomainEvent {

    private final String bounceReason;

    public EmailBouncedEvent(UUID emailId, String bounceReason) {
        super(emailId);
        this.bounceReason = bounceReason;
    }

    public String getBounceReason() { return bounceReason; }
}
