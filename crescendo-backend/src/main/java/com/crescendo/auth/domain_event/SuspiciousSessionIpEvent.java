package com.crescendo.auth.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Domain event published when a user session is refreshed from an IP address
 * that drastically differs from the original IP address.
 */
public class SuspiciousSessionIpEvent extends BaseDomainEvent {
    
    private final UUID sessionId;
    private final String originalIp;
    private final String newIp;

    public SuspiciousSessionIpEvent(UUID userId, UUID sessionId, String originalIp, String newIp) {
        super(userId);
        this.sessionId = sessionId;
        this.originalIp = originalIp;
        this.newIp = newIp;
    }

    public UUID getSessionId() { return sessionId; }
    public String getOriginalIp() { return originalIp; }
    public String getNewIp() { return newIp; }
}
