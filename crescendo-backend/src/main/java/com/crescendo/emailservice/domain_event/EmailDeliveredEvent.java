package com.crescendo.emailservice.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Raised when delivery confirmation is received from the email provider.
 */
public class EmailDeliveredEvent extends BaseDomainEvent {

    private final UUID domainId;

    public EmailDeliveredEvent(UUID emailId, UUID domainId) {
        super(emailId);
        this.domainId = domainId;
    }

    public UUID getDomainId() { return domainId; }
}
