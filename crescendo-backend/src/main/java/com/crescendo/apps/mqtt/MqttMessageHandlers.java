package com.crescendo.apps.mqtt;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import tools.jackson.databind.ObjectMapper;

@Component
public class MqttMessageHandlers {
    private final ObjectMapper mapper;

    public MqttMessageHandlers(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @ActionMapping(appKey = "mqtt", actionKey = "publish")
    public ActionResult publishMessage(ActionContext c) {
        MqttClient client = null;
        try {
            String id = cred(c, "clientId", "");
            if (id.isBlank()) {
                id = MqttClient.generateClientId();
            }

            client = new MqttClient(cred(c, "brokerUrl", ""), id, null);

            MqttConnectOptions opt = new MqttConnectOptions();
            if (!cred(c, "username", "").isBlank()) {
                opt.setUserName(cred(c, "username", ""));
            }
            if (!cred(c, "password", "").isBlank()) {
                opt.setPassword(cred(c, "password", "").toCharArray());
            }

            client.connect(opt);

            String topic = String.valueOf(c.configuration().get("topic"));
            
            boolean sendInputData = "true".equalsIgnoreCase(String.valueOf(c.configuration().getOrDefault("sendInputData", "true")));
            String payload;
            if (sendInputData) {
                payload = mapper.writeValueAsString(c.input());
            } else {
                payload = String.valueOf(c.configuration().getOrDefault("message", ""));
            }
            
            MqttMessage msg = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            msg.setQos(intVal(c.configuration().get("qos"), 0));
            msg.setRetained(Boolean.parseBoolean(
                    String.valueOf(c.configuration().getOrDefault("retain", "false"))));

            client.publish(topic, msg);

            return ActionResult.success(Map.of(
                    "published", true,
                    "topic", topic));
        } catch (Exception e) {
            return ActionResult.failure("MQTT publish failed: " + e.getMessage());
        } finally {
            if (client != null) {
                try {
                    if (client.isConnected()) {
                        client.disconnect();
                    }
                    client.close();
                } catch (Exception ignored) {
                    // Best-effort cleanup
                }
            }
        }
    }

    private String cred(ActionContext c, String k, String fallback) {
        Object v = c.credentials() != null ? c.credentials().get(k) : null;
        return v == null ? fallback : String.valueOf(v);
    }

    private int intVal(Object v, int fallback) {
        try {
            return v == null ? fallback : Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return fallback;
        }
    }
}
