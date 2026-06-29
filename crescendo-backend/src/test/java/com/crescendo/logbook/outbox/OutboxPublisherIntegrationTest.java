package com.crescendo.logbook.outbox;

import com.crescendo.BaseIntegrationTest;
import com.crescendo.shared.infrastructure.event.RedisDomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OutboxPublisherIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @MockitoBean
    private RedisDomainEventPublisher redisPublisher;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
    }

    @Test
    void publishOutboxEvents_success_marksAsPublished() {
        OutboxEvent event = new OutboxEvent(UUID.randomUUID(), "test-stream", Map.of("key", "value"));
        event = outboxEventRepository.save(event);

        doReturn(org.springframework.data.redis.connection.stream.RecordId.of("1-0")).when(redisPublisher).enqueueToStreamOrThrow(anyString(), any());

        outboxPublisher.publishOutboxEvents();

        Optional<OutboxEvent> result = outboxEventRepository.findById(event.getId());
        assertTrue(result.isPresent());
        assertTrue(result.get().isPublished());
        assertEquals(0, result.get().getAttemptCount());
        
        verify(redisPublisher, times(1)).enqueueToStreamOrThrow(eq("test-stream"), anyMap());
    }

    @Test
    void publishOutboxEvents_failure_incrementsAttemptAndMarksDLQ() {
        OutboxEvent event = new OutboxEvent(UUID.randomUUID(), "test-stream", Map.of("key", "value"));
        event = outboxEventRepository.save(event);

        doThrow(new RuntimeException("Redis down")).when(redisPublisher).enqueueToStreamOrThrow(anyString(), any());

        // First 4 failures
        for (int i = 1; i <= 4; i++) {
            outboxPublisher.publishOutboxEvents();
            OutboxEvent current = outboxEventRepository.findById(event.getId()).orElseThrow();
            assertFalse(current.isPublished());
            assertEquals(i, current.getAttemptCount());
        }

        // 5th failure marks it as published (DLQ)
        outboxPublisher.publishOutboxEvents();
        OutboxEvent finalEvent = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertTrue(finalEvent.isPublished(), "Should be marked published to prevent infinite retries");
        assertEquals(5, finalEvent.getAttemptCount());

        // 6th run shouldn't pick it up
        outboxPublisher.publishOutboxEvents();
        verify(redisPublisher, times(5)).enqueueToStreamOrThrow(anyString(), any());
    }
}
