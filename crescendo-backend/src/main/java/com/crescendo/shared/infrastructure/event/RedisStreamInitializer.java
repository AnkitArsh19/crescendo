package com.crescendo.shared.infrastructure.event;

import com.crescendo.config.RedisStreamConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Creates Redis Streams and consumer groups on application startup.
 * Runs once after the application context is fully initialized.
 *
 * If streams/groups already exist (e.g., container restart), creation is silently skipped.
 */
@Component
public class RedisStreamInitializer {

    private static final Logger logger = LoggerFactory.getLogger(RedisStreamInitializer.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisStreamInitializer(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeStreams() {
        createStreamAndGroup(RedisStreamConfig.STREAM_WORKFLOW_EVENTS);
        createStreamAndGroup(RedisStreamConfig.STREAM_USER_EVENTS);
        createStreamAndGroup(RedisStreamConfig.STREAM_AUTH_EVENTS);
        createStreamAndGroup(RedisStreamConfig.STREAM_EXECUTION_QUEUE);
        createStreamAndGroup(RedisStreamConfig.STREAM_EMAIL_QUEUE);
        createStreamAndGroup(RedisStreamConfig.STREAM_DEAD_LETTER);

        logger.info("Redis Streams and consumer groups initialized");
    }

    private void createStreamAndGroup(String streamKey) {
        try {
            redisTemplate.opsForStream().createGroup(streamKey, ReadOffset.from("0"), RedisStreamConfig.CONSUMER_GROUP);
            logger.debug("Created stream and consumer group for: {}", streamKey);
        } catch (RedisSystemException e) {
            // BUSYGROUP = consumer group already exists (safe to ignore on restart)
            if (e.getCause() != null && e.getCause().getMessage() != null
                    && e.getCause().getMessage().contains("BUSYGROUP")) {
                logger.debug("Consumer group already exists for stream: {}", streamKey);
            } else {
                logger.warn("Failed to create stream/group for {}: {}", streamKey, e.getMessage());
            }
        }
    }
}
