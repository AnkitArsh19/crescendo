package com.crescendo.shared.infrastructure.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

/**
 * Periodically checks the Redis Stream listener container and restarts it if needed.
 */
@Component
public class StreamContainerHealthCheck {

    private static final Logger logger = LoggerFactory.getLogger(StreamContainerHealthCheck.class);

    private final StreamMessageListenerContainer<String, MapRecord<String, Object, Object>> container;

    public StreamContainerHealthCheck(
            StreamMessageListenerContainer<String, MapRecord<String, Object, Object>> container) {
        this.container = container;
    }

    @Scheduled(fixedRate = 30_000)
    public void ensureRunning() {
        if (container.isRunning()) {
            return;
        }

        logger.warn("[health] Redis Stream container not running — attempting restart");
        try {
            container.start();
            logger.info("[health] Redis Stream container restarted");
        } catch (Exception e) {
            logger.error("[health] Failed to restart Redis Stream container: {}", e.getMessage());
        }
    }
}
