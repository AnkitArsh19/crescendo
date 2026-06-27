package com.crescendo.apps.rabbitmq;

import com.crescendo.execution.trigger.TriggerPoller;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RabbitMqMessageReceivedPoller implements TriggerPoller {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMqMessageReceivedPoller.class);
    private final ObjectMapper mapper;

    public RabbitMqMessageReceivedPoller(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean supports(String appKey, String triggerKey) {
        return "rabbitmq".equals(appKey) && "message-received".equals(triggerKey);
    }

    @Override
    public List<Map<String, Object>> poll(Map<String, Object> credentials, Map<String, Object> configuration, Instant lastPollTime) {
        List<Map<String, Object>> events = new ArrayList<>();
        if (credentials == null || configuration == null) return events;

        try {
            ConnectionFactory f = new ConnectionFactory();
            f.setHost(cred(credentials, "host", ""));
            f.setPort(Integer.parseInt(cred(credentials, "port", "5672")));
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

            try (Connection conn = f.newConnection(); Channel ch = conn.createChannel()) {
                String q = String.valueOf(configuration.getOrDefault("queue", ""));
                if (q.isBlank()) return events;

                ch.queueDeclare(q, true, false, false, null);

                String bindingExchange = String.valueOf(configuration.getOrDefault("bindingExchange", ""));
                String bindingRoutingKey = String.valueOf(configuration.getOrDefault("bindingRoutingKey", ""));
                
                if (!bindingExchange.isBlank() && !"null".equals(bindingExchange)) {
                    ch.queueBind(q, bindingExchange, bindingRoutingKey.isBlank() || "null".equals(bindingRoutingKey) ? "" : bindingRoutingKey);
                }

                int maxMessages = intVal(configuration.get("maxMessages"), 10);
                boolean jsonParseBody = "true".equalsIgnoreCase(String.valueOf(configuration.getOrDefault("jsonParseBody", "false")));
                boolean onlyContent = "true".equalsIgnoreCase(String.valueOf(configuration.getOrDefault("onlyContent", "false")));
                boolean contentIsBinary = "true".equalsIgnoreCase(String.valueOf(configuration.getOrDefault("contentIsBinary", "false")));

                for (int i = 0; i < maxMessages; i++) {
                    GetResponse response = ch.basicGet(q, true);
                    if (response == null) {
                        break;
                    }

                    Object parsedBody = null;
                    if (contentIsBinary) {
                        parsedBody = java.util.Base64.getEncoder().encodeToString(response.getBody());
                    } else {
                        String msgStr = new String(response.getBody(), StandardCharsets.UTF_8);
                        parsedBody = msgStr;
                        if (jsonParseBody) {
                            try {
                                parsedBody = mapper.readValue(msgStr, Object.class);
                            } catch (Exception e) {
                                // Ignore parse error, fallback to string
                            }
                        }
                    }

                    Map<String, Object> payload = new LinkedHashMap<>();
                    if (onlyContent) {
                        payload.put("content", parsedBody);
                    } else {
                        payload.put("content", parsedBody);
                        payload.put("exchange", response.getEnvelope().getExchange());
                        payload.put("routingKey", response.getEnvelope().getRoutingKey());
                        payload.put("messageCount", response.getMessageCount());
                    }
                    events.add(payload);
                }
            }
        } catch (Exception e) {
            logger.error("[rabbitmq-poller] Failed to poll RabbitMQ queue", e);
        }

        return events;
    }

    private String cred(Map<String, Object> c, String k, String f) {
        Object v = c != null ? c.get(k) : null;
        return v == null || String.valueOf(v).isBlank() ? f : String.valueOf(v);
    }

    private int intVal(Object v, int fallback) {
        try {
            return v == null ? fallback : Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return fallback;
        }
    }
}
