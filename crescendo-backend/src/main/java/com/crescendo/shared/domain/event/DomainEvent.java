package com.crescendo.shared.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for all domain events.
 * Domain events represent something that happened in the domain that domain experts care about.
 */
public interface DomainEvent {

    /**
     * Unique identifier for this event occurrence.
     */
    UUID eventId();

    /**
     * When the event occurred.
     */
    Instant occurredAt();

    /**
     * The aggregate ID that this event relates to.
     */
    UUID aggregateId();

    /**
     * The type of event for routing/serialization.
     */
    default String eventType() {
        return this.getClass().getSimpleName();
    }
}
