package com.crescendo.shared.infrastructure.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Holds only the SSE connections accepted by this application instance. Redis
 * Pub/Sub carries workflow change notifications to every instance, which then
 * fan them out to their own local connections.
 */
@Service
public class WorkflowSseService implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(WorkflowSseService.class);
    private static final String CHANNEL_PREFIX = "workflow-events:";

    private final StringRedisTemplate redisTemplate;
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public WorkflowSseService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public SseEmitter connect(UUID userId) {
        SseEmitter emitter = new SseEmitter(0L);
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

    public void notifyUser(UUID userId, UUID workflowId, String eventType) {
        if (userId == null) return;
        redisTemplate.convertAndSend(CHANNEL_PREFIX + userId, workflowId + "|" + eventType);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        if (!channel.startsWith(CHANNEL_PREFIX)) return;

        try {
            UUID userId = UUID.fromString(channel.substring(CHANNEL_PREFIX.length()));
            String[] parts = payload.split("\\|", 2);
            UUID workflowId = UUID.fromString(parts[0]);
            String eventType = parts.length == 2 ? parts[1] : "UPDATED";
            sendToLocalEmitters(userId, new WorkflowChangedEvent(workflowId, eventType));
        } catch (RuntimeException exception) {
            log.warn("Ignoring malformed workflow SSE notification on {}", channel, exception);
        }
    }

    private void sendToLocalEmitters(UUID userId, WorkflowChangedEvent event) {
        for (SseEmitter emitter : emitters.getOrDefault(userId, new CopyOnWriteArrayList<>())) {
            try {
                emitter.send(SseEmitter.event().name("workflow-changed").data(event));
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

    public record WorkflowChangedEvent(UUID workflowId, String type) {}
}
