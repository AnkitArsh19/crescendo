package com.crescendo.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages active Server-Sent Events (SSE) connections for user notifications.
 * Uses Redis Pub/Sub to broadcast notifications across multiple application instances.
 */
@Service
public class NotificationSseService implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationSseService.class);
    public static final String CHANNEL_PREFIX = "user-notifications:";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final StringRedisTemplate redisTemplate;
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public NotificationSseService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public SseEmitter connect(UUID userId) {
        SseEmitter emitter = new SseEmitter(0L); // 0L means no server-side timeout
        CopyOnWriteArrayList<SseEmitter> userEmitters = emitters.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>());
        userEmitters.add(emitter);

        Runnable remove = () -> removeEmitter(userId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(ignored -> remove.run());

        try {
            emitter.send(SseEmitter.event().name("connected").data("ready"));
        } catch (IOException exception) {
            remove.run();
        }
        return emitter;
    }

    public void broadcastNotification(UUID userId, UserNotificationDto notification) {
        if (userId == null || notification == null) return;
        try {
            String payload = OBJECT_MAPPER.writeValueAsString(notification);
            redisTemplate.convertAndSend(CHANNEL_PREFIX + userId, payload);
        } catch (Exception e) {
            log.error("Failed to publish user notification to Redis channel for user {}: {}", userId, e.getMessage());
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        if (!channel.startsWith(CHANNEL_PREFIX)) return;

        try {
            UUID userId = UUID.fromString(channel.substring(CHANNEL_PREFIX.length()));
            UserNotificationDto dto = OBJECT_MAPPER.readValue(payload, UserNotificationDto.class);
            sendToLocalEmitters(userId, dto);
        } catch (Exception exception) {
            log.warn("Ignoring malformed notification SSE payload on {}: {}", channel, exception.getMessage());
        }
    }

    private void sendToLocalEmitters(UUID userId, UserNotificationDto event) {
        CopyOnWriteArrayList<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(event));
            } catch (IOException | IllegalStateException exception) {
                removeEmitter(userId, emitter);
            }
        }
    }

    private void removeEmitter(UUID userId, SseEmitter emitter) {
        emitters.computeIfPresent(userId, (ignored, userEmitters) -> {
            userEmitters.remove(emitter);
            return userEmitters.isEmpty() ? null : userEmitters;
        });
    }
}
