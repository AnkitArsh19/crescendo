package com.crescendo.emailservice.domain_event;

import com.crescendo.shared.domain.event.DomainEvent;
import java.util.UUID;

public record DmarcAlignmentFailureEvent(UUID domainId, String domainName, String orgName, String sourceIp) implements DomainEvent {
    @Override
    public UUID aggregateId() {
        return domainId;
    }

    @Override
    public java.time.Instant occurredAt() {
        return java.time.Instant.now();
    }

    @Override
    public UUID eventId() {
        return UUID.randomUUID();
    }
}
