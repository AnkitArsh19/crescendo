package com.crescendo.logbook.outbox;

import com.crescendo.shared.infrastructure.event.RedisDomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Polls the {@code outbox_event} table and publishes unpublished events to Redis Streams.
 *
 * <p><strong>Max attempts:</strong> Events that fail {@value #MAX_ATTEMPTS} times are
 * marked as permanently failed and skipped. Without this threshold, a single
 * poison event (bad payload, unreachable stream key) would be retried every
 * 5 seconds forever, and because {@code findBatchForUpdate} uses
 * {@code PESSIMISTIC_WRITE}, it would hold a row-level lock on the same rows
 * during every Redis I/O attempt — turning a downed Redis into a database
 * lock storm.</p>
 *
 * <p><strong>Cleanup:</strong> Published and permanently-failed events older than
 * 7 days are deleted to prevent unbounded table growth.</p>
 */
@Component
public class OutboxPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OutboxPublisher.class);

    /**
     * Maximum number of publish attempts before an event is considered permanently failed.
     * After this many failures, the event is marked as published (terminal) and logged
     * as an error for manual investigation.
     */
    private static final int MAX_ATTEMPTS = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final RedisDomainEventPublisher redisDomainEventPublisher;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                           RedisDomainEventPublisher redisDomainEventPublisher) {
        this.outboxEventRepository = outboxEventRepository;
        this.redisDomainEventPublisher = redisDomainEventPublisher;
    }

    /**
     * Polls for unpublished outbox events every 5 seconds.
     *
     * <p>Events exceeding {@value #MAX_ATTEMPTS} attempts are excluded by the
     * repository query, so they are never locked or retried.</p>
     */
    @Scheduled(fixedRate = 5_000)
    @Transactional
    public void publishOutboxEvents() {
        List<OutboxEvent> unpublished = outboxEventRepository.findBatchForUpdate(MAX_ATTEMPTS, PageRequest.of(0, 100));
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
                int newCount = event.getAttemptCount() + 1;
                event.setAttemptCount(newCount);
                event.setLastAttemptAt(Instant.now());

                if (newCount >= MAX_ATTEMPTS) {
                    // Mark as published (terminal) so it's never retried.
                    // A dedicated 'failed' flag could be added, but marking as
                    // published is sufficient since the cleanup job deletes old rows.
                    event.setPublished(true);
                    logger.error("[outbox] Event {} permanently failed after {} attempts " +
                                    "(stream={}, payload keys={}): {}",
                            event.getId(), newCount, event.getStreamKey(),
                            event.getPayload().keySet(), e.getMessage());
                } else {
                    logger.warn("[outbox] Event {} failed (attempt {}/{}): {}",
                            event.getId(), newCount, MAX_ATTEMPTS, e.getMessage());
                }

                outboxEventRepository.save(event);
            }
        }
    }

    /**
     * Deletes published outbox events older than 7 days.
     * Runs once per hour to prevent unbounded table growth.
     */
    @Scheduled(fixedRate = 3_600_000) // Every hour
    @Transactional
    public void cleanupPublishedEvents() {
        Instant threshold = Instant.now().minus(7, ChronoUnit.DAYS);
        int deleted = outboxEventRepository.deletePublishedBefore(threshold);
        if (deleted > 0) {
            logger.info("[outbox] Cleaned up {} published outbox events older than 7 days", deleted);
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