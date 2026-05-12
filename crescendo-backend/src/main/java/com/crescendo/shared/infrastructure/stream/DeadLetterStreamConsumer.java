package com.crescendo.shared.infrastructure.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Redis Stream consumer for the dead-letter queue.
 *
 * Consumes from {@code crescendo:dlq} and retries failed events by
 * re-publishing them to their original stream. Events that have exceeded
 * the maximum retry count are logged and discarded.
 */
@Component
public class DeadLetterStreamConsumer implements StreamListener<String, MapRecord<String, Object, Object>> {

    private static final Logger logger = LoggerFactory.getLogger(DeadLetterStreamConsumer.class);
    private static final int MAX_RETRIES = 3;

    private final RedisTemplate<String, Object> redisTemplate;

    public DeadLetterStreamConsumer(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void onMessage(MapRecord<String, Object, Object> message) {
        Map<Object, Object> raw = message.getValue();
        String originalStream = unquote(raw.get("originalStream") != null ? raw.get("originalStream").toString() : null);
        String eventType = unquote(raw.get("eventType") != null ? raw.get("eventType").toString() : null);
        String aggregateId = unquote(raw.get("aggregateId") != null ? raw.get("aggregateId").toString() : null);
        int retryCount = parseRetryCount(raw.get("retryCount") != null ? raw.get("retryCount").toString() : null);
        long timestamp = raw.get("timestamp") != null ? Long.parseLong(unquote(raw.get("timestamp").toString())) : System.currentTimeMillis();

        logger.info("[dlq] Processing dead-letter event: type={}, aggregateId={}, retryCount={}/{}",
                eventType, aggregateId, retryCount, MAX_RETRIES);

        if (retryCount >= MAX_RETRIES) {
            logger.error("[dlq] Event exhausted retries ({}x), discarding: type={}, aggregateId={}, error={}",
                    retryCount, eventType, aggregateId, raw.get("error"));
            return;
        }

        if (originalStream == null) {
            logger.warn("[dlq] Dead-letter event missing originalStream, cannot retry: {}", raw);
            return;
        }

        long backoffMs = (long) Math.pow(2, retryCount) * 5000L; // Exponential backoff: 5s, 10s, 20s
        long elapsed = System.currentTimeMillis() - timestamp;
        if (elapsed < backoffMs) {
            try {
                // Sleep for the remaining backoff time to prevent immediate looping
                Thread.sleep(backoffMs - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        // Re-publish to original stream with incremented retry count
        Map<String, String> retryData = new HashMap<>();
        raw.forEach((k, v) -> retryData.put(String.valueOf(k), String.valueOf(v)));
        retryData.put("retryCount", String.valueOf(retryCount + 1));
        retryData.remove("error");
        retryData.remove("originalStream");

        try {
            RecordId recordId = redisTemplate.opsForStream()
                    .add(StreamRecords.newRecord()
                            .ofMap(retryData)
                            .withStreamKey(originalStream));

            logger.info("[dlq] Retried event to stream {}: type={}, aggregateId={}, newId={}",
                    originalStream, eventType, aggregateId, recordId);
        } catch (Exception e) {
            logger.error("[dlq] Failed to retry event: type={}, aggregateId={}, error={}",
                    eventType, aggregateId, e.getMessage());
        }
    }

    private int parseRetryCount(String value) {
        if (value == null) return 0;
        try {
            return Integer.parseInt(unquote(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String unquote(String s) {
        if (s != null && s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
