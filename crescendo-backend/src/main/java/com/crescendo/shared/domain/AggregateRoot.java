package com.crescendo.shared.domain;

import com.crescendo.shared.domain.event.DomainEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Base class for Aggregate Roots in DDD.
 * An aggregate root is the entry point to an aggregate - a cluster of domain objects
 * that are treated as a single unit for data changes.
 * Features:
 * - Identity via UUID
 * - Domain event collection and publishing
 * - Equality based on identity
 */
public abstract class AggregateRoot {

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * Returns the unique identifier for this aggregate.
     */
    public abstract UUID getId();

    /**
     * Register a domain event to be published after the transaction commits.
     */
    protected void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    /**
     * Get all pending domain events.
     */
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /**
     * Clear all domain events after they have been published.
     */
    public void clearDomainEvents() {
        domainEvents.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AggregateRoot that = (AggregateRoot) o;
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : 0;
    }
}
