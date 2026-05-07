package com.crescendo.emailservice.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Raised when an email send attempt fails.
 */
public class EmailFailedEvent extends BaseDomainEvent {

    private final String error;

    public EmailFailedEvent(UUID emailId, String error) {
        super(emailId);
        this.error = error;
    }

    public String getError() { return error; }
}
