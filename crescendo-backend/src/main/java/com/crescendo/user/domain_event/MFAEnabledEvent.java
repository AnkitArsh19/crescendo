package com.crescendo.user.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Domain event raised when MFA is enabled for a user account.
 */
public class MFAEnabledEvent extends BaseDomainEvent {

    public MFAEnabledEvent(UUID userId) {
        super(userId);
    }
}
