package com.crescendo.shared.domain.event;

/**
 * Interface for publishing domain events.
 * Implementations can use Spring's ApplicationEventPublisher or message queues.
 */
public interface DomainEventPublisher {

    /**
     * Publish a domain event.
     */
    void publish(DomainEvent event);

    /**
     * Publish multiple domain events.
     */
    default void publishAll(Iterable<DomainEvent> events) {
        events.forEach(this::publish);
    }
}
