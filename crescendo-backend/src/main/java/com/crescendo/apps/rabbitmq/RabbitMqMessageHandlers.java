package com.crescendo.apps.rabbitmq;

import com.crescendo.execution.action.*;
import com.rabbitmq.client.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class RabbitMqMessageHandlers {
    private final tools.jackson.databind.ObjectMapper mapper;

    public RabbitMqMessageHandlers(tools.jackson.databind.ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @ActionMapping(appKey = "rabbitmq", actionKey = "publish")
    public ActionResult publishMessage(ActionContext c) {
        try {
            ConnectionFactory f = new ConnectionFactory();
            f.setHost(cred(c, "host", ""));
            f.setPort(Integer.parseInt(cred(c, "port", "5672")));
            if (!cred(c, "username", "").isBlank()) {
                f.setUsername(cred(c, "username", ""));
            }
            if (!cred(c, "password", "").isBlank()) {
                f.setPassword(cred(c, "password", ""));
            }
            f.setVirtualHost(cred(c, "virtualHost", "/"));
            if (Boolean.parseBoolean(cred(c, "ssl", "false"))) {
                f.useSslProtocol();
            }

            try (Connection conn = f.newConnection(); Channel ch = conn.createChannel()) {
                String operation = String.valueOf(c.configuration().getOrDefault("operation", "sendMessage"));
                
                if ("deleteMessage".equalsIgnoreCase(operation)) {
                    String q = String.valueOf(c.configuration().getOrDefault("queue", ""));
                    GetResponse response = ch.basicGet(q, true);
                    if (response == null) {
                        return ActionResult.success(Map.of("deleted", false, "reason", "Queue is empty"));
                    }
                    return ActionResult.success(Map.of("deleted", true, "messageCount", response.getMessageCount()));
                }

                String mode = String.valueOf(c.configuration().getOrDefault("mode", "queue"));
                
                boolean sendInputData = "true".equalsIgnoreCase(String.valueOf(c.configuration().getOrDefault("sendInputData", "true")));
                String payload;
                if (sendInputData) {
                    payload = mapper.writeValueAsString(c.input());
                } else {
                    payload = String.valueOf(c.configuration().getOrDefault("message", ""));
                }
                
                AMQP.BasicProperties.Builder propsBuilder = new AMQP.BasicProperties.Builder();
                if ("true".equalsIgnoreCase(String.valueOf(c.configuration().getOrDefault("durable", "true")))) {
                    propsBuilder.deliveryMode(2); // persistent
                }
                String messageId = String.valueOf(c.configuration().getOrDefault("messageId", ""));
                if (!messageId.isBlank()) propsBuilder.messageId(messageId);
                
                String headersStr = String.valueOf(c.configuration().getOrDefault("headers", ""));
                if (!headersStr.isBlank() && !"null".equals(headersStr)) {
                    try {
                        Map<String, Object> headers = mapper.readValue(headersStr, new tools.jackson.core.type.TypeReference<Map<String, Object>>() {});
                        propsBuilder.headers(headers);
                    } catch (Exception e) {}
                }

                if ("exchange".equalsIgnoreCase(mode)) {
                    String exchange = String.valueOf(c.configuration().getOrDefault("exchange", ""));
                    String exchangeType = String.valueOf(c.configuration().getOrDefault("exchangeType", "direct"));
                    String routingKey = String.valueOf(c.configuration().getOrDefault("routingKey", ""));
                    
                    String argumentsStr = String.valueOf(c.configuration().getOrDefault("arguments", ""));
                    Map<String, Object> arguments = null;
                    if (!argumentsStr.isBlank() && !"null".equals(argumentsStr)) {
                        try {
                            arguments = mapper.readValue(argumentsStr, new tools.jackson.core.type.TypeReference<Map<String, Object>>() {});
                        } catch (Exception e) {}
                    }
                    
                    ch.exchangeDeclare(exchange, exchangeType, "true".equalsIgnoreCase(String.valueOf(c.configuration().getOrDefault("durable", "true"))), false, arguments);
                    
                    ch.basicPublish(exchange, routingKey, propsBuilder.build(), payload.getBytes(StandardCharsets.UTF_8));
                } else {
                    String q = String.valueOf(c.configuration().getOrDefault("queue", ""));
                    ch.queueDeclare(q, true, false, false, null);
                    ch.basicPublish("", q, propsBuilder.build(), payload.getBytes(StandardCharsets.UTF_8));
                }
                return ActionResult.success(Map.of("published", true, "mode", mode));
            }
        } catch (Exception e) {
            return ActionResult.failure("RabbitMQ publish failed: " + e.getMessage());
        }
    }

    String cred(ActionContext c, String k, String f) {
        Object v = c.credentials() != null ? c.credentials().get(k) : null;
        return v == null || String.valueOf(v).isBlank() ? f : String.valueOf(v);
    }
}
