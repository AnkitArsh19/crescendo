package com.crescendo.shared.infrastructure.stream;

import com.crescendo.config.RedisStreamConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Redis Stream consumer for user and auth domain events.
 *
 * Consumes from {@code crescendo:events:user} and {@code crescendo:events:auth}.
 * Provides durable async processing of user lifecycle and security events.
 *
 * The synchronous {@code @TransactionalEventListener} in UserEventListener handles
 * immediate query-side projection and cache eviction. This consumer adds durability
 * and enables cross-service event propagation.
 */
@Component
public class UserStreamConsumer implements StreamListener<String, MapRecord<String, Object, Object>> {

    private static final Logger logger = LoggerFactory.getLogger(UserStreamConsumer.class);

    @Override
    public void onMessage(MapRecord<String, Object, Object> message) {
        Map<Object, Object> raw = message.getValue();
        String eventType = unquote(String.valueOf(raw.getOrDefault("eventType", "unknown")));
        String aggregateId = unquote(String.valueOf(raw.getOrDefault("aggregateId", "unknown")));
        String streamKey = message.getStream();

        logger.debug("User/auth stream event: type={}, aggregateId={}, stream={}, streamId={}",
                eventType, aggregateId, streamKey, message.getId());

        if (RedisStreamConfig.STREAM_AUTH_EVENTS.equals(streamKey)) {
            handleAuthEvent(eventType, aggregateId, raw);
        } else {
            handleUserEvent(eventType, aggregateId, raw);
        }
    }

    private void handleUserEvent(String eventType, String aggregateId, Map<Object, Object> data) {
        switch (eventType) {
            case "UserRegisteredEvent" ->
                    logger.info("[stream] User registered: userId={}", aggregateId);
            case "UserEmailVerifiedEvent" ->
                    logger.info("[stream] Email verified: userId={}", aggregateId);
            case "UserProfileUpdatedEvent" ->
                    logger.info("[stream] Profile updated: userId={}", aggregateId);
            case "UserAccountDeletedEvent" ->
                    logger.info("[stream] Account deleted: userId={}", aggregateId);
            default ->
                    logger.debug("[stream] User event: type={}, userId={}", eventType, aggregateId);
        }
    }

    private void handleAuthEvent(String eventType, String aggregateId, Map<Object, Object> data) {
        switch (eventType) {
            case "UserLoggedInEvent" ->
                    logger.info("[stream] User logged in: userId={}", aggregateId);
            case "UserPasswordChangedEvent" ->
                    logger.info("[stream] Password changed: userId={}", aggregateId);
            case "UserPasswordResetEvent" ->
                    logger.info("[stream] Password reset: userId={}", aggregateId);
            case "MFAEnabledEvent" ->
                    logger.info("[stream] MFA enabled: userId={}", aggregateId);
            case "MFADisabledEvent" ->
                    logger.info("[stream] MFA disabled: userId={}", aggregateId);
            case "OAuthProviderLinkedEvent" ->
                    logger.info("[stream] OAuth linked: userId={}", aggregateId);
            default ->
                    logger.debug("[stream] Auth event: type={}, userId={}", eventType, aggregateId);
        }
    }
    private static String unquote(String s) {
        if (s != null && s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
