package com.crescendo.auth.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Domain event raised when a user changes their password (authenticated change, not reset).
 */
public class UserPasswordChangedEvent extends BaseDomainEvent {

    public UserPasswordChangedEvent(UUID userId) {
        super(userId);
    }
}
