package com.crescendo.emailservice.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Raised when delivery confirmation is received from the email provider.
 */
public class EmailDeliveredEvent extends BaseDomainEvent {

    public EmailDeliveredEvent(UUID emailId) {
        super(emailId);
    }
}
