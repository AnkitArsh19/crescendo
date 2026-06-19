package com.crescendo.execution.trigger;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Component
public class MqttTriggerPoller implements TriggerPoller {
    @Override
    public boolean supports(String appKey, String triggerKey) {
        return "mqtt".equals(appKey) && "message-received".equals(triggerKey);
    }

    @Override
    public List<Map<String, Object>> poll(Map<String, Object> credentials,
                                          Map<String, Object> configuration,
                                          Instant lastPollTime) {
        String brokerUrl = cred(credentials, "brokerUrl", "");
        String topic = cfg(configuration, "topic", "");
        if (brokerUrl.isBlank() || topic.isBlank()) {
            return List.of();
        }

        int maxMessages = Math.max(1, Math.min(intVal(configuration.get("maxMessages"), 10), 100));
        int listenSeconds = Math.max(1, Math.min(intVal(configuration.get("listenSeconds"), 10), 30));
        List<Map<String, Object>> messages = java.util.Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(maxMessages);
        MqttClient client = null;

        try {
            String configuredClientId = cred(credentials, "clientId", "");
            String clientId = configuredClientId.isBlank()
                    ? "crescendo-trigger-" + UUID.randomUUID()
                    : configuredClientId + "-trigger";
            client = new MqttClient(brokerUrl, clientId, null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            if (!cred(credentials, "username", "").isBlank()) {
                options.setUserName(cred(credentials, "username", ""));
            }
            if (!cred(credentials, "password", "").isBlank()) {
                options.setPassword(cred(credentials, "password", "").toCharArray());
            }

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                }

                @Override
                public void messageArrived(String topicName, MqttMessage message) {
                    if (messages.size() >= maxMessages) {
                        return;
                    }
                    messages.add(event(topicName, message));
                    latch.countDown();
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });
            client.connect(options);
            client.subscribe(topic, Math.max(0, Math.min(intVal(configuration.get("qos"), 0), 2)));
            latch.await(listenSeconds, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            return List.of();
        } finally {
            try {
                if (client != null && client.isConnected()) {
                    client.disconnect();
                }
                if (client != null) {
                    client.close();
                }
            } catch (Exception ignored) {
            }
        }

        return List.copyOf(messages);
    }

    private Map<String, Object> event(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        return Map.of(
                "id", UUID.randomUUID().toString(),
                "topic", topic,
                "payload", payload,
                "qos", message.getQos(),
                "retained", message.isRetained(),
                "createdAt", Instant.now().toString()
        );
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
