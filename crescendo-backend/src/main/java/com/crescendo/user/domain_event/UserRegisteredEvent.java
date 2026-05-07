package com.crescendo.user.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Domain event raised when a new user registers.
 */
public class UserRegisteredEvent extends BaseDomainEvent {

    private final String email;
    private final String username;

    public UserRegisteredEvent(UUID userId, String email, String username) {
        super(userId);
        this.email = email;
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }
}
