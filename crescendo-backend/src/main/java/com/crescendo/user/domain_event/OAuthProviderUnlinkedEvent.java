package com.crescendo.user.domain_event;

import com.crescendo.enums.AuthProvider;
import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Domain event raised when an OAuth provider is unlinked from a user account.
 */
public class OAuthProviderUnlinkedEvent extends BaseDomainEvent {

    private final AuthProvider provider;

    public OAuthProviderUnlinkedEvent(UUID userId, AuthProvider provider) {
        super(userId);
        this.provider = provider;
    }

    public AuthProvider getProvider() { return provider; }
}
