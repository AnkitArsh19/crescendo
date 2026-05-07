package com.crescendo.auth.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Domain event raised when a user completes a password reset via token.
 */
public class UserPasswordResetEvent extends BaseDomainEvent {

    public UserPasswordResetEvent(UUID userId) {
        super(userId);
    }
}
