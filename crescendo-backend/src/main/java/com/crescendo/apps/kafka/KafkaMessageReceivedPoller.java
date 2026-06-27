package com.crescendo.apps.kafka;

import com.crescendo.execution.trigger.TriggerPoller;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
public class KafkaMessageReceivedPoller implements TriggerPoller {
    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageReceivedPoller.class);
    private final ObjectMapper mapper;

    public KafkaMessageReceivedPoller(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean supports(String appKey, String triggerKey) {
        return "kafka".equals(appKey) && "message-received".equals(triggerKey);
    }

    @Override
    public List<Map<String, Object>> poll(Map<String, Object> credentials, Map<String, Object> configuration, Instant lastPollTime) {
        List<Map<String, Object>> events = new ArrayList<>();
        if (credentials == null || configuration == null) return events;

        String topic = String.valueOf(configuration.getOrDefault("topic", ""));
        if (topic.isBlank()) return events;

        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, cred(credentials, "brokers", ""));
        p.put(ConsumerConfig.GROUP_ID_CONFIG, configuration.getOrDefault("groupId", "crescendo-workflow"));
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        
        boolean fromBeginning = "true".equalsIgnoreCase(String.valueOf(configuration.getOrDefault("fromBeginning", "false")));
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, fromBeginning ? "earliest" : "latest");
        
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

        if (Boolean.parseBoolean(cred(credentials, "ssl", "false"))) {
            p.put("security.protocol", "SSL");
        }
        if (!cred(credentials, "username", "").isBlank()) {
            p.put("security.protocol", Boolean.parseBoolean(cred(credentials, "ssl", "false")) ? "SASL_SSL" : "SASL_PLAINTEXT");
            p.put("sasl.mechanism", cred(credentials, "saslMechanism", "PLAIN"));
            p.put("sasl.jaas.config",
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\""
                            + cred(credentials, "username", "")
                            + "\" password=\"" + cred(credentials, "password", "") + "\";");
        }

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(p)) {
            consumer.subscribe(List.of(topic));
            
            int pollSeconds = intVal(configuration.get("pollSeconds"), 5);
            int maxMessages = intVal(configuration.get("maxMessages"), 10);
            
            boolean jsonParseMessage = "true".equalsIgnoreCase(String.valueOf(configuration.getOrDefault("jsonParseMessage", "false")));
            boolean onlyMessage = "true".equalsIgnoreCase(String.valueOf(configuration.getOrDefault("onlyMessage", "false")));
            boolean returnHeaders = "true".equalsIgnoreCase(String.valueOf(configuration.getOrDefault("returnHeaders", "false")));

            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(pollSeconds));
            int count = 0;
            
            for (ConsumerRecord<String, String> record : records) {
                if (count >= maxMessages) break;
                
                Object parsedBody = record.value();
                if (jsonParseMessage && record.value() != null) {
                    try {
                        parsedBody = mapper.readValue(record.value(), Object.class);
                    } catch (Exception e) {}
                }

                Map<String, Object> payload = new LinkedHashMap<>();
                if (onlyMessage) {
                    payload.put("message", parsedBody);
                } else {
                    payload.put("message", parsedBody);
                    payload.put("topic", record.topic());
                    payload.put("partition", record.partition());
                    payload.put("offset", record.offset());
                    payload.put("key", record.key());
                    if (returnHeaders) {
                        Map<String, String> h = new LinkedHashMap<>();
                        for (Header header : record.headers()) {
                            h.put(header.key(), new String(header.value(), StandardCharsets.UTF_8));
                        }
                        payload.put("headers", h);
                    }
                }
                events.add(payload);
                count++;
            }
        } catch (Exception e) {
            logger.error("[kafka-poller] Failed to poll Kafka topic", e);
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
