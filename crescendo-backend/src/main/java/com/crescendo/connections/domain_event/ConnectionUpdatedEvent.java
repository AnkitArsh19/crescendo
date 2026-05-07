package com.crescendo.connections.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

public class ConnectionUpdatedEvent extends BaseDomainEvent {

    public ConnectionUpdatedEvent(UUID connectionId) {
        super(connectionId);
    }

    @Override
    public String eventType() { return "CONNECTION_UPDATED"; }
}
