package com.crescendo.shared.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base record for domain events with common fields.
 * Extend this for specific domain events.
 */
public abstract class BaseDomainEvent implements DomainEvent {

    private final UUID eventId;
    private final Instant occurredAt;
    private final UUID aggregateId;

    protected BaseDomainEvent(UUID aggregateId) {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
        this.aggregateId = aggregateId;
    }

    protected BaseDomainEvent(UUID eventId, Instant occurredAt, UUID aggregateId) {
        this.eventId = eventId;
        this.occurredAt = occurredAt;
        this.aggregateId = aggregateId;
    }

    @Override
    public UUID eventId() {
        return eventId;
    }

    @Override
    public Instant occurredAt() {
        return occurredAt;
    }

    @Override
    public UUID aggregateId() {
        return aggregateId;
    }
}
