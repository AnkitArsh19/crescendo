package com.crescendo.user.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Domain event raised when MFA is disabled for a user account.
 */
public class MFADisabledEvent extends BaseDomainEvent {

    public MFADisabledEvent(UUID userId) {
        super(userId);
    }
}
