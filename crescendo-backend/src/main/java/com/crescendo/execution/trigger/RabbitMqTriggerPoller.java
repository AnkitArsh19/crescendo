package com.crescendo.execution.trigger;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RabbitMqTriggerPoller implements TriggerPoller {
    @Override
    public boolean supports(String appKey, String triggerKey) {
        return "rabbitmq".equals(appKey) && "message-received".equals(triggerKey);
    }

    @Override
    public List<Map<String, Object>> poll(Map<String, Object> credentials,
                                          Map<String, Object> configuration,
                                          Instant lastPollTime) {
        String queue = cfg(configuration, "queue", "");
        if (queue.isBlank()) {
            return List.of();
        }
        int maxMessages = Math.max(1, Math.min(intVal(configuration.get("maxMessages"), 10), 100));
        java.util.ArrayList<Map<String, Object>> events = new java.util.ArrayList<>();

        try (Connection conn = factory(credentials).newConnection();
             Channel channel = conn.createChannel()) {
            for (int i = 0; i < maxMessages; i++) {
                GetResponse response = channel.basicGet(queue, true);
                if (response == null) {
                    break;
                }
                events.add(event(queue, response));
            }
        } catch (Exception ignored) {
            return List.of();
        }

        return events;
    }

    private ConnectionFactory factory(Map<String, Object> credentials) throws Exception {
        ConnectionFactory f = new ConnectionFactory();
        f.setHost(cred(credentials, "host", ""));
        f.setPort(intVal(cred(credentials, "port", "5672"), 5672));
        if (!cred(credentials, "username", "").isBlank()) {
            f.setUsername(cred(credentials, "username", ""));
        }
        if (!cred(credentials, "password", "").isBlank()) {
            f.setPassword(cred(credentials, "password", ""));
        }
        f.setVirtualHost(cred(credentials, "virtualHost", "/"));
        if (Boolean.parseBoolean(cred(credentials, "ssl", "false"))) {
            f.useSslProtocol();
        }
        return f;
    }

    private Map<String, Object> event(String queue, GetResponse response) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", queue + "-" + response.getEnvelope().getDeliveryTag());
        out.put("queue", queue);
        out.put("body", new String(response.getBody(), StandardCharsets.UTF_8));
        out.put("exchange", response.getEnvelope().getExchange());
        out.put("routingKey", response.getEnvelope().getRoutingKey());
        out.put("redeliver", response.getEnvelope().isRedeliver());
        out.put("headers", response.getProps().getHeaders() != null ? response.getProps().getHeaders() : Map.of());
        out.put("createdAt", Instant.now().toString());
        return out;
    }

    private String cfg(Map<String, Object> config, String key, String fallback) {
        Object value = config != null ? config.get(key) : null;
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    private String cred(Map<String, Object> credentials, String key, String fallback) {
        Object value = credentials != null ? credentials.get(key) : null;
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    private int intVal(Object value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return fallback;
        }
    }
}
