package com.crescendo.user.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Domain event raised when a user permanently deletes their account.
 */
public class UserAccountDeletedEvent extends BaseDomainEvent {

    public UserAccountDeletedEvent(UUID userId) {
        super(userId);
    }
}
