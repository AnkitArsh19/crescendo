package com.crescendo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.time.Duration;

/**
 * Configures Redis Streams for async event/queue processing.
 *
 * Stream names (keys):
 *   - crescendo:events:workflow   — workflow lifecycle events (created, activated, deleted, etc.)
 *   - crescendo:events:user       — user lifecycle events (registered, verified, etc.)
 *   - crescendo:events:auth       — security/auth events (logins, password changes, MFA)
 *   - crescendo:queue:execution   — workflow execution queue (steps to run)
 *   - crescendo:dlq               — dead-letter queue for failed events
 *
 * Consumer groups are created lazily by RedisStreamInitializer on startup.
 * Stream listeners are registered by StreamConsumerRegistrar after the container starts.
 */
@Configuration
public class RedisStreamConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedisStreamConfig.class);

    /** Stream key for workflow domain events. */
    public static final String STREAM_WORKFLOW_EVENTS = "crescendo:events:workflow";

    /** Stream key for user domain events. */
    public static final String STREAM_USER_EVENTS = "crescendo:events:user";

    /** Stream key for security/auth domain events (logins, password changes, MFA). */
    public static final String STREAM_AUTH_EVENTS = "crescendo:events:auth";

    /** Stream key for workflow execution queue. */
    public static final String STREAM_EXECUTION_QUEUE = "crescendo:queue:execution";

    /** Stream key for email sending queue. */
    public static final String STREAM_EMAIL_QUEUE = "crescendo:queue:email";

    /** Stream key for dead-letter queue (failed events for retry). */
    public static final String STREAM_DEAD_LETTER = "crescendo:dlq";

    /** Consumer group for the backend service. */
    public static final String CONSUMER_GROUP = "crescendo-backend";

    /**
     * Listener container for consuming Redis Streams.
     * Polls streams every 500ms and dispatches to registered StreamListener beans.
     * Uses MapRecord since events are published as hash maps.
     */
    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, Object, Object>> streamListenerContainer(
            RedisConnectionFactory connectionFactory) {

        @SuppressWarnings("unchecked")
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, Object, Object>> options =
                StreamMessageListenerContainer
                .StreamMessageListenerContainerOptions.<String, MapRecord<String, Object, Object>>builder()
                .pollTimeout(Duration.ofMillis(500))
                .keySerializer((RedisSerializer<String>) (RedisSerializer<?>) new StringRedisSerializer())
                .hashKeySerializer((RedisSerializer<Object>) (RedisSerializer<?>) new StringRedisSerializer())
                .hashValueSerializer((RedisSerializer<Object>) (RedisSerializer<?>) new StringRedisSerializer())
                .build();

        StreamMessageListenerContainer<String, MapRecord<String, Object, Object>> container =
                StreamMessageListenerContainer.create(connectionFactory, options);
        container.start();

        logger.info("Redis Stream listener container started");
        return container;
    }
}
