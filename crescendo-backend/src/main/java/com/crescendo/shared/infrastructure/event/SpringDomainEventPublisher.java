package com.crescendo.shared.infrastructure.event;

import com.crescendo.shared.domain.event.DomainEvent;
import com.crescendo.shared.domain.event.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Spring-based implementation of DomainEventPublisher.
 * Publishes events both in-process (Spring ApplicationEventPublisher) and
 * to Redis Streams (RedisDomainEventPublisher) for async/durable processing.
 */
@Component
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(SpringDomainEventPublisher.class);
    private final ApplicationEventPublisher applicationEventPublisher;
    private final RedisDomainEventPublisher redisDomainEventPublisher;

    public SpringDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher,
                                      RedisDomainEventPublisher redisDomainEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.redisDomainEventPublisher = redisDomainEventPublisher;
    }

    @Override
    public void publish(DomainEvent event) {
        logger.debug("Publishing domain event: {} for aggregate: {}", 
                event.eventType(), event.aggregateId());
        // In-process listeners (synchronous within the same transaction)
        applicationEventPublisher.publishEvent(event);
        // Redis Stream (async, durable — consumed by stream listeners)
        redisDomainEventPublisher.publish(event);
    }
}
