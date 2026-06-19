package com.crescendo.shared.infrastructure.stream;

import com.crescendo.config.RedisStreamConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class ExecutionQueuePendingReaper {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionQueuePendingReaper.class);
    private static final Duration MIN_IDLE_TIME = Duration.ofMinutes(5);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ExecutionQueueConsumer executionQueueConsumer;

    public ExecutionQueuePendingReaper(RedisTemplate<String, Object> redisTemplate,
                                       ExecutionQueueConsumer executionQueueConsumer) {
        this.redisTemplate = redisTemplate;
        this.executionQueueConsumer = executionQueueConsumer;
    }

    @Scheduled(fixedRate = 60_000)
    public void reclaimPendingMessages() {
        try {
            // Find messages pending for a long time
            PendingMessages pendingMessages = redisTemplate.opsForStream().pending(
                    RedisStreamConfig.STREAM_EXECUTION_QUEUE,
                    RedisStreamConfig.CONSUMER_GROUP,
                    Range.unbounded(),
                    100L);

            if (pendingMessages == null || pendingMessages.isEmpty()) {
                return;
            }

            for (PendingMessage pendingMessage : pendingMessages) {
                if (pendingMessage.getElapsedTimeSinceLastDelivery().compareTo(MIN_IDLE_TIME) > 0) {
                    logger.info("[execution-reaper] Found stalled message {}, claiming it...", pendingMessage.getId());
                    
                    // Claim the message
                    List<MapRecord<String, Object, Object>> claimed = redisTemplate.opsForStream().claim(
                            RedisStreamConfig.STREAM_EXECUTION_QUEUE,
                            RedisStreamConfig.CONSUMER_GROUP,
                            "consumer-1", // default consumer name in StreamConsumerRegistrar
                            MIN_IDLE_TIME,
                            pendingMessage.getId());

                    if (claimed != null && !claimed.isEmpty()) {
                        for (MapRecord<String, Object, Object> record : claimed) {
                            logger.info("[execution-reaper] Claimed message {}, delegating to consumer...", record.getId());
                            executionQueueConsumer.onMessage(record);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("[execution-reaper] Failed to reclaim pending messages: {}", e.getMessage(), e);
        }
    }
}
