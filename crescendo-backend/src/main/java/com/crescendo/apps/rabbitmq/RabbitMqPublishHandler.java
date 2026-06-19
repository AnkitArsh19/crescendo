package com.crescendo.apps.rabbitmq;

import com.crescendo.execution.action.*;
import com.rabbitmq.client.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@ActionMapping(appKey = "rabbitmq", actionKey = "publish")
public class RabbitMqPublishHandler implements ActionHandler {
    @Override
    public ActionResult execute(ActionContext c) {
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
                String mode = String.valueOf(c.configuration().getOrDefault("mode", "queue"));
                String msg = String.valueOf(c.configuration().getOrDefault("message", ""));

                if ("exchange".equalsIgnoreCase(mode)) {
                    ch.basicPublish(
                            String.valueOf(c.configuration().getOrDefault("exchange", "")),
                            String.valueOf(c.configuration().getOrDefault("routingKey", "")),
                            null,
                            msg.getBytes(StandardCharsets.UTF_8)
                    );
                } else {
                    String q = String.valueOf(c.configuration().getOrDefault("queue", ""));
                    ch.queueDeclare(q, true, false, false, null);
                    ch.basicPublish("", q, MessageProperties.PERSISTENT_TEXT_PLAIN, msg.getBytes(StandardCharsets.UTF_8));
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
