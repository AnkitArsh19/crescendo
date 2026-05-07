package com.crescendo.user.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Domain event raised when a user's sessions are revoked (single or bulk).
 */
public class UserSessionRevokedEvent extends BaseDomainEvent {

    private final boolean allSessions;

    public UserSessionRevokedEvent(UUID userId, boolean allSessions) {
        super(userId);
        this.allSessions = allSessions;
    }

    public boolean isAllSessions() { return allSessions; }
}
