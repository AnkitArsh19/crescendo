package com.crescendo.auth.domain_event;

import com.crescendo.enums.AuthProvider;
import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Domain event raised when an OAuth provider is linked to an existing account.
 */
public class OAuthProviderLinkedEvent extends BaseDomainEvent {

    private final AuthProvider provider;

    public OAuthProviderLinkedEvent(UUID userId, AuthProvider provider) {
        super(userId);
        this.provider = provider;
    }

    public AuthProvider getProvider() { return provider; }
}
