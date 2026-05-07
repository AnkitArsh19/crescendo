package com.crescendo.shared.infrastructure.stream;

import com.crescendo.config.RedisStreamConfig;
import com.crescendo.emailservice.queue.EmailQueueConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;

/**
 * Registers Redis Stream consumers with the listener container on application startup.
 *
 * Each consumer reads from its assigned stream(s) using the shared consumer group.
 *
 * <p><strong>ACK strategy:</strong></p>
 * <ul>
 *   <li><strong>Execution queue</strong> — manual ACK ({@code receive}). The consumer
 *       acknowledges AFTER successful processing so that lock-contention or processing
 *       failures leave the message as a pending entry for automatic redelivery.</li>
 *   <li><strong>All other streams</strong> — auto ACK ({@code receiveAutoAck}).
 *       These do lightweight, non-critical work (logging, cache eviction).</li>
 * </ul>
 *
 * Registration order:
 *   1. Workflow events stream  → WorkflowStreamConsumer
 *   2. User events stream      → UserStreamConsumer
 *   3. Auth events stream      → UserStreamConsumer (shared handler)
 *   4. Execution queue stream  → ExecutionQueueConsumer (manual ACK)
 *   5. Email queue stream       → EmailQueueConsumer
 *   6. Dead-letter stream      → DeadLetterStreamConsumer
 */
@Component
public class StreamConsumerRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(StreamConsumerRegistrar.class);
    private static final String CONSUMER_NAME = "consumer-1";

    private final StreamMessageListenerContainer<String, MapRecord<String, Object, Object>> container;
    private final WorkflowStreamConsumer workflowConsumer;
    private final UserStreamConsumer userConsumer;
    private final ExecutionQueueConsumer executionConsumer;
    private final EmailQueueConsumer emailConsumer;
    private final DeadLetterStreamConsumer deadLetterConsumer;

    public StreamConsumerRegistrar(
            StreamMessageListenerContainer<String, MapRecord<String, Object, Object>> container,
            WorkflowStreamConsumer workflowConsumer,
            UserStreamConsumer userConsumer,
            ExecutionQueueConsumer executionConsumer,
            EmailQueueConsumer emailConsumer,
            DeadLetterStreamConsumer deadLetterConsumer) {
        this.container = container;
        this.workflowConsumer = workflowConsumer;
        this.userConsumer = userConsumer;
        this.executionConsumer = executionConsumer;
        this.emailConsumer = emailConsumer;
        this.deadLetterConsumer = deadLetterConsumer;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerConsumers() {
        Consumer consumer = Consumer.from(RedisStreamConfig.CONSUMER_GROUP, CONSUMER_NAME);

        container.receiveAutoAck(consumer,
                StreamOffset.create(RedisStreamConfig.STREAM_WORKFLOW_EVENTS, ReadOffset.lastConsumed()),
                workflowConsumer);

        container.receiveAutoAck(consumer,
                StreamOffset.create(RedisStreamConfig.STREAM_USER_EVENTS, ReadOffset.lastConsumed()),
                userConsumer);

        container.receiveAutoAck(consumer,
                StreamOffset.create(RedisStreamConfig.STREAM_AUTH_EVENTS, ReadOffset.lastConsumed()),
                userConsumer);

        // Execution queue: manual ACK — message stays pending until processing succeeds.
        // If the consumer fails to process (e.g., lock contention, crash), the message
        // is automatically redelivered on the next poll via the pending entries list.
        container.receive(consumer,
                StreamOffset.create(RedisStreamConfig.STREAM_EXECUTION_QUEUE, ReadOffset.lastConsumed()),
                executionConsumer);

        container.receiveAutoAck(consumer,
                StreamOffset.create(RedisStreamConfig.STREAM_EMAIL_QUEUE, ReadOffset.lastConsumed()),
                emailConsumer);

        container.receiveAutoAck(consumer,
                StreamOffset.create(RedisStreamConfig.STREAM_DEAD_LETTER, ReadOffset.lastConsumed()),
                deadLetterConsumer);

        logger.info("Registered {} Redis Stream consumers across {} streams (execution queue uses manual ACK)",
                6, 6);
    }
}
