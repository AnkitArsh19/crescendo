package com.crescendo.auth.domain_event;

import com.crescendo.enums.AuthProvider;
import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Domain event raised when a user successfully logs in (local or OAuth).
 */
public class UserLoggedInEvent extends BaseDomainEvent {

    private final String email;
    private final AuthProvider provider;

    public UserLoggedInEvent(UUID userId, String email, AuthProvider provider) {
        super(userId);
        this.email = email;
        this.provider = provider;
    }

    public String getEmail() { return email; }
    public AuthProvider getProvider() { return provider; }
}
