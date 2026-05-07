package com.crescendo.user.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Domain event raised when a user updates their profile (username, etc.).
 */
public class UserProfileUpdatedEvent extends BaseDomainEvent {

    private final String newUsername;

    public UserProfileUpdatedEvent(UUID userId, String newUsername) {
        super(userId);
        this.newUsername = newUsername;
    }

    public String getNewUsername() { return newUsername; }
}
