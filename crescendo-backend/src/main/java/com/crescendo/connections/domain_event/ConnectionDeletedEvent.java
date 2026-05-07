package com.crescendo.connections.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

public class ConnectionDeletedEvent extends BaseDomainEvent {

    public ConnectionDeletedEvent(UUID connectionId) {
        super(connectionId);
    }

    @Override
    public String eventType() { return "CONNECTION_DELETED"; }
}
