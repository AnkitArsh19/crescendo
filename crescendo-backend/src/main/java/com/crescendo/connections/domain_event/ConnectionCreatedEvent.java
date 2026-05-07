package com.crescendo.connections.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

public class ConnectionCreatedEvent extends BaseDomainEvent {

    private final UUID userId;
    private final String appKey;

    public ConnectionCreatedEvent(UUID connectionId, UUID userId, String appKey) {
        super(connectionId);
        this.userId = userId;
        this.appKey = appKey;
    }

    public UUID getUserId() { return userId; }
    public String getAppKey() { return appKey; }

    @Override
    public String eventType() { return "CONNECTION_CREATED"; }
}
