package com.crescendo.auth.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Domain event raised when a new user session (refresh token) is created.
 * Published by JWTService after persisting the UserSession row.
 *
 * <p>Subscribers can use this to:
 * <ul>
 *   <li>Send login-from-new-device security alerts</li>
 *   <li>Enforce max concurrent session limits</li>
 *   <li>Populate analytics / audit trail</li>
 * </ul>
 */
public class UserSessionCreatedEvent extends BaseDomainEvent {

    private final UUID sessionId;
    private final String clientIp;
    private final String deviceLabel;

    public UserSessionCreatedEvent(UUID userId, UUID sessionId, String clientIp, String deviceLabel) {
        super(userId);
        this.sessionId = sessionId;
        this.clientIp = clientIp;
        this.deviceLabel = deviceLabel;
    }

    public UUID getSessionId() { return sessionId; }
    public String getClientIp() { return clientIp; }
    public String getDeviceLabel() { return deviceLabel; }
}
