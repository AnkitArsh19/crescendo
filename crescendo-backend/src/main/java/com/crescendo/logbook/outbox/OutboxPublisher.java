package com.crescendo.logbook.outbox;

import com.crescendo.shared.infrastructure.event.RedisDomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OutboxPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final RedisDomainEventPublisher redisDomainEventPublisher;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                           RedisDomainEventPublisher redisDomainEventPublisher) {
        this.outboxEventRepository = outboxEventRepository;
        this.redisDomainEventPublisher = redisDomainEventPublisher;
    }

    @Scheduled(fixedRate = 5_000)
    @Transactional
    public void publishOutboxEvents() {
        List<OutboxEvent> unpublished = outboxEventRepository.findBatchForUpdate(PageRequest.of(0, 100));
        if (unpublished.isEmpty()) {
            return;
        }

        for (OutboxEvent event : unpublished) {
            try {
                redisDomainEventPublisher.enqueueToStreamOrThrow(
                        event.getStreamKey(),
                        toStringMap(event.getPayload()));
                event.setPublished(true);
                event.setLastAttemptAt(Instant.now());
                outboxEventRepository.save(event);
            } catch (Exception e) {
                logger.error("Failed to publish outbox event {}: {}", event.getId(), e.getMessage());
                event.setAttemptCount(event.getAttemptCount() + 1);
                event.setLastAttemptAt(Instant.now());
                outboxEventRepository.save(event);
            }
        }
    }

    private Map<String, String> toStringMap(Map<String, Object> payload) {
        Map<String, String> converted = new HashMap<>();
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (entry.getValue() != null) {
                converted.put(entry.getKey(), entry.getValue().toString());
            }
        }
        return converted;
    }
}