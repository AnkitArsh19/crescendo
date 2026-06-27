package com.crescendo.apps.mqtt;

import com.crescendo.execution.trigger.TriggerPoller;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class MqttMessageReceivedPoller implements TriggerPoller {
    private static final Logger logger = LoggerFactory.getLogger(MqttMessageReceivedPoller.class);
    private final ObjectMapper mapper;

    public MqttMessageReceivedPoller(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean supports(String appKey, String triggerKey) {
        return "mqtt".equals(appKey) && "message-received".equals(triggerKey);
    }

    @Override
    public List<Map<String, Object>> poll(Map<String, Object> credentials, Map<String, Object> configuration, Instant lastPollTime) {
        List<Map<String, Object>> events = new ArrayList<>();
        if (credentials == null || configuration == null) return events;

        MqttClient client = null;
        try {
            String id = cred(credentials, "clientId", "");
            if (id.isBlank()) id = MqttClient.generateClientId();

            client = new MqttClient(cred(credentials, "brokerUrl", ""), id, null);

            MqttConnectOptions opt = new MqttConnectOptions();
            if (!cred(credentials, "username", "").isBlank()) {
                opt.setUserName(cred(credentials, "username", ""));
            }
            if (!cred(credentials, "password", "").isBlank()) {
                opt.setPassword(cred(credentials, "password", "").toCharArray());
            }

            client.connect(opt);

            String topicsStr = String.valueOf(configuration.getOrDefault("topics", ""));
            if (topicsStr.isBlank()) {
                logger.warn("[mqtt-poller] No topics provided");
                return events;
            }

            int defaultQos = intVal(configuration.get("qos"), 0);
            String[] topicConfigs = topicsStr.split(",");
            String[] topicFilters = new String[topicConfigs.length];
            int[] qosLevels = new int[topicConfigs.length];

            for (int i = 0; i < topicConfigs.length; i++) {
                String tc = topicConfigs[i].trim();
                String[] parts = tc.split(":");
                topicFilters[i] = parts[0].trim();
                qosLevels[i] = parts.length > 1 ? intVal(parts[1], defaultQos) : defaultQos;
            }

            boolean jsonParseBody = "true".equalsIgnoreCase(String.valueOf(configuration.getOrDefault("jsonParseBody", "false")));
            boolean onlyMessage = "true".equalsIgnoreCase(String.valueOf(configuration.getOrDefault("onlyMessage", "false")));
            
            int listenSeconds = intVal(configuration.get("listenSeconds"), 10);
            int maxMessages = intVal(configuration.get("maxMessages"), 10);

            CountDownLatch latch = new CountDownLatch(maxMessages);
            AtomicInteger count = new AtomicInteger(0);

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {}

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    if (count.getAndIncrement() < maxMessages) {
                        try {
                            String msgStr = new String(message.getPayload(), StandardCharsets.UTF_8);
                            Object parsedBody = msgStr;
                            if (jsonParseBody) {
                                try {
                                    parsedBody = mapper.readValue(msgStr, Object.class);
                                } catch (Exception e) {
                                    // Keep as string if parsing fails
                                }
                            }

                            if (onlyMessage) {
                                Map<String, Object> payload = new LinkedHashMap<>();
                                payload.put("message", parsedBody);
                                synchronized (events) {
                                    events.add(payload);
                                }
                            } else {
                                Map<String, Object> payload = new LinkedHashMap<>();
                                payload.put("message", parsedBody);
                                payload.put("topic", topic);
                                synchronized (events) {
                                    events.add(payload);
                                }
                            }
                        } finally {
                            latch.countDown();
                        }
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            client.subscribe(topicFilters, qosLevels);
            latch.await(listenSeconds, TimeUnit.SECONDS);

        } catch (Exception e) {
            logger.error("[mqtt-poller] Polling failed", e);
        } finally {
            if (client != null) {
                try {
                    if (client.isConnected()) {
                        client.disconnect();
                    }
                    client.close();
                } catch (Exception ignored) {}
            }
        }

        return events;
    }

    private String cred(Map<String, Object> creds, String k, String fallback) {
        Object v = creds != null ? creds.get(k) : null;
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
