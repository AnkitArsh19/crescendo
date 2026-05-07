package com.crescendo.user.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Domain event raised when a user's email is verified.
 */
public class UserEmailVerifiedEvent extends BaseDomainEvent {

    private final String email;

    public UserEmailVerifiedEvent(UUID userId, String email) {
        super(userId);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
