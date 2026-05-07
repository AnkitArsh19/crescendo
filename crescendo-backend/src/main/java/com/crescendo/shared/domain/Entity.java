package com.crescendo.shared.domain;

import java.util.UUID;

/**
 * Base class for Entity objects in DDD.
 * Entities have identity and are mutable. They are equal if their IDs are equal.
 */
public abstract class Entity {

    /**
     * Returns the unique identifier for this entity.
     */
    public abstract UUID getId();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity entity = (Entity) o;
        return getId() != null && getId().equals(entity.getId());
    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : 0;
    }
}
