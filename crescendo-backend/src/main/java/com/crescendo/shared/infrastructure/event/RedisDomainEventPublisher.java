package com.crescendo.shared.infrastructure.event;

import com.crescendo.config.RedisStreamConfig;
import com.crescendo.shared.domain.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Publishes domain events to Redis Streams for async processing by consumers.
 *
 * This is a complement to SpringDomainEventPublisher — Spring events handle
 * synchronous in-process listeners, while this pushes to Redis Streams for
 * cross-process / async / durable consumption (e.g., workflow execution queue).
 *
 * Stream routing:
 *   - Events with type starting with "Workflow" → crescendo:events:workflow
 *   - Events with type starting with "User"     → crescendo:events:user
 *   - Auth-related events                       → crescendo:events:auth
 *   - All others                                → crescendo:events:workflow (default)
 *
 * Failed events are routed to the dead-letter queue for retry.
 */
@Component
public class RedisDomainEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(RedisDomainEventPublisher.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisDomainEventPublisher(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Publish a domain event to the appropriate Redis Stream.
     * On failure, the event is sent to the dead-letter queue for retry.
     */
    public void publish(DomainEvent event) {
        String streamKey = resolveStream(event);
        Map<String, String> eventData = serializeEvent(event);

        try {
            RecordId recordId = redisTemplate.opsForStream()
                    .add(StreamRecords.newRecord()
                            .ofMap(eventData)
                            .withStreamKey(streamKey));

            logger.debug("Published event {} to stream {} with ID {}",
                    event.eventType(), streamKey, recordId);
        } catch (Exception e) {
            logger.error("Failed to publish event {} to Redis stream {}: {}",
                    event.eventType(), streamKey, e.getMessage());
            sendToDeadLetter(eventData, streamKey, e.getMessage());
        }
    }

    /**
     * Publish a workflow execution request to the execution queue stream.
     */
    public void enqueueExecution(Map<String, String> executionData) {
        try {
            RecordId recordId = enqueueToStreamOrThrow(RedisStreamConfig.STREAM_EXECUTION_QUEUE, executionData);
            logger.debug("Enqueued execution to {} with ID {}",
                    RedisStreamConfig.STREAM_EXECUTION_QUEUE, recordId);
        } catch (Exception e) {
            logger.error("Failed to enqueue execution: {}", e.getMessage());
            sendToDeadLetter(executionData, RedisStreamConfig.STREAM_EXECUTION_QUEUE, e.getMessage());
        }
    }

    /**
     * Publish a request to a specific Redis stream.
     * Throws on failure so callers can decide whether to retry.
     */
    public RecordId enqueueToStreamOrThrow(String streamKey, Map<String, String> data) {
        return redisTemplate.opsForStream()
                .add(StreamRecords.newRecord()
                        .ofMap(data)
                        .withStreamKey(streamKey));
    }

    /**
     * Publish an email send request to the email queue stream.
     */
    public void enqueueEmail(Map<String, String> emailData) {
        try {
            RecordId recordId = redisTemplate.opsForStream()
                    .add(StreamRecords.newRecord()
                            .ofMap(emailData)
                            .withStreamKey(RedisStreamConfig.STREAM_EMAIL_QUEUE));

            logger.debug("Enqueued email to {} with ID {}",
                    RedisStreamConfig.STREAM_EMAIL_QUEUE, recordId);
        } catch (Exception e) {
            logger.error("Failed to enqueue email: {}", e.getMessage());
            sendToDeadLetter(emailData, RedisStreamConfig.STREAM_EMAIL_QUEUE, e.getMessage());
        }
    }

    /**
     * Send a failed event to the dead-letter queue for later retry.
     */
    private void sendToDeadLetter(Map<String, String> eventData, String originalStream, String error) {
        try {
            Map<String, String> dlqData = new HashMap<>(eventData);
            dlqData.put("originalStream", originalStream);
            dlqData.put("error", error);
            dlqData.put("retryCount", eventData.getOrDefault("retryCount", "0"));
            dlqData.put("timestamp", String.valueOf(System.currentTimeMillis()));

            redisTemplate.opsForStream()
                    .add(StreamRecords.newRecord()
                            .ofMap(dlqData)
                            .withStreamKey(RedisStreamConfig.STREAM_DEAD_LETTER));

            logger.info("Sent failed event to DLQ: type={}, aggregateId={}",
                    eventData.get("eventType"), eventData.get("aggregateId"));
        } catch (Exception dlqEx) {
            logger.error("CRITICAL: Failed to send event to DLQ: {}", dlqEx.getMessage());
        }
    }

    private String resolveStream(DomainEvent event) {
        String type = event.eventType();
        // Email service events go to the email queue for async processing
        if (type.startsWith("Email") || type.startsWith("Domain") || type.startsWith("ApiKey")) {
            return RedisStreamConfig.STREAM_WORKFLOW_EVENTS;
        }
        // Security-relevant events go to the auth stream
        if (type.startsWith("UserLoggedIn") || type.startsWith("UserPassword")
                || type.startsWith("OAuth") || type.startsWith("MFA")) {
            return RedisStreamConfig.STREAM_AUTH_EVENTS;
        }
        // User lifecycle events (registration, verification, profile, deletion)
        if (type.startsWith("User")) {
            return RedisStreamConfig.STREAM_USER_EVENTS;
        }
        // Everything else (workflow, step, logbook events)
        return RedisStreamConfig.STREAM_WORKFLOW_EVENTS;
    }

    private Map<String, String> serializeEvent(DomainEvent event) {
        Map<String, String> data = new HashMap<>();
        data.put("eventId", event.eventId().toString());
        data.put("eventType", event.eventType());
        data.put("aggregateId", event.aggregateId().toString());
        data.put("occurredAt", event.occurredAt().toString());
        return data;
    }
}
