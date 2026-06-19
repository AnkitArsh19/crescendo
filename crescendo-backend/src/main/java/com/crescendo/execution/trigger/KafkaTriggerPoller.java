package com.crescendo.execution.trigger;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

@Component
public class KafkaTriggerPoller implements TriggerPoller {
    @Override
    public boolean supports(String appKey, String triggerKey) {
        return "kafka".equals(appKey) && "message-received".equals(triggerKey);
    }

    @Override
    public List<Map<String, Object>> poll(Map<String, Object> credentials,
                                          Map<String, Object> configuration,
                                          Instant lastPollTime) {
        String topic = cfg(configuration, "topic", "");
        String brokers = cred(credentials, "brokers", "");
        if (topic.isBlank() || brokers.isBlank()) {
            return List.of();
        }

        int maxMessages = Math.max(1, Math.min(intVal(configuration.get("maxMessages"), 10), 100));
        int pollSeconds = Math.max(1, Math.min(intVal(configuration.get("pollSeconds"), 5), 30));
        List<Map<String, Object>> events = new ArrayList<>();

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props(credentials, configuration))) {
            consumer.subscribe(List.of(topic));
            Instant deadline = Instant.now().plusSeconds(pollSeconds);
            while (events.size() < maxMessages && Instant.now().isBefore(deadline)) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, String> record : records) {
                    events.add(event(record));
                    if (events.size() >= maxMessages) {
                        break;
                    }
                }
            }
        } catch (Exception ignored) {
            return List.of();
        }

        return events;
    }

    private Properties props(Map<String, Object> credentials, Map<String, Object> configuration) {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, cred(credentials, "brokers", ""));
        p.put(ConsumerConfig.GROUP_ID_CONFIG, cfg(configuration, "groupId", "crescendo-workflow"));
        p.put(ConsumerConfig.CLIENT_ID_CONFIG, cred(credentials, "clientId", "crescendo-trigger"));
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        boolean ssl = Boolean.parseBoolean(cred(credentials, "ssl", "false"));
        if (ssl) {
            p.put("security.protocol", "SSL");
        }
        if (!cred(credentials, "username", "").isBlank()) {
            p.put("security.protocol", ssl ? "SASL_SSL" : "SASL_PLAINTEXT");
            p.put("sasl.mechanism", cred(credentials, "saslMechanism", "PLAIN"));
            p.put("sasl.jaas.config",
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\""
                            + cred(credentials, "username", "") + "\" password=\""
                            + cred(credentials, "password", "") + "\";");
        }
        return p;
    }

    private Map<String, Object> event(ConsumerRecord<String, String> record) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", record.topic() + "-" + record.partition() + "-" + record.offset());
        out.put("topic", record.topic());
        out.put("partition", record.partition());
        out.put("offset", record.offset());
        out.put("key", record.key());
        out.put("value", record.value());
        out.put("timestamp", Instant.ofEpochMilli(record.timestamp()).toString());
        out.put("headers", headers(record));
        out.put("createdAt", Instant.now().toString());
        return out;
    }

    private Map<String, Object> headers(ConsumerRecord<String, String> record) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Header header : record.headers()) {
            byte[] value = header.value();
            out.put(header.key(), value == null ? null : new String(value, StandardCharsets.UTF_8));
        }
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
