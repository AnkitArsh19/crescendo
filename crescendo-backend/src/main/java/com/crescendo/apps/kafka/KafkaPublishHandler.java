package com.crescendo.apps.kafka;

import com.crescendo.execution.action.*;
import tools.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringSerializer;

import java.nio.charset.StandardCharsets;
import java.util.*;

@ActionMapping(appKey = "kafka", actionKey = "publish")
public class KafkaPublishHandler implements ActionHandler {
    private final ObjectMapper mapper;

    public KafkaPublishHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try (Producer<String, String> producer = new KafkaProducer<>(props(c))) {
            ProducerRecord<String, String> rec = new ProducerRecord<>(String.valueOf(c.configuration().get("topic")),
                    empty(c.configuration().get("key")) ? null : String.valueOf(c.configuration().get("key")),
                    String.valueOf(c.configuration().getOrDefault("message", "")));
            headers(c).forEach((k, v) -> rec.headers()
                    .add(new RecordHeader(k, String.valueOf(v).getBytes(StandardCharsets.UTF_8))));
            RecordMetadata m = producer.send(rec).get();
            return ActionResult.success(Map.of("topic", m.topic(), "partition", m.partition(), "offset", m.offset()));
        } catch (Exception e) {
            return ActionResult.failure("Kafka publish failed: " + e.getMessage());
        }
    }

    Properties props(ActionContext c) {
        Properties p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, cred(c, "brokers", ""));
        p.put(ProducerConfig.CLIENT_ID_CONFIG, cred(c, "clientId", "crescendo"));
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        if (Boolean.parseBoolean(cred(c, "ssl", "false"))) {
            p.put("security.protocol", "SSL");
        }
        if (!cred(c, "username", "").isBlank()) {
            p.put("security.protocol",
                    Boolean.parseBoolean(cred(c, "ssl", "false")) ? "SASL_SSL" : "SASL_PLAINTEXT");
            p.put("sasl.mechanism", cred(c, "saslMechanism", "PLAIN"));
            p.put("sasl.jaas.config",
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\""
                            + cred(c, "username", "")
                            + "\" password=\"" + cred(c, "password", "") + "\";");
        }
        return p;
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> headers(ActionContext c) throws Exception {
        Object h = c.configuration().get("headers");
        if (h == null) {
            return Map.of();
        }
        if (h instanceof Map<?, ?> m) {
            Map<String, Object> out = new LinkedHashMap<>();
            m.forEach((k, v) -> out.put(String.valueOf(k), v));
            return out;
        }
        return mapper.readValue(String.valueOf(h), Map.class);
    }

    String cred(ActionContext c, String k, String f) {
        Object v = c.credentials() != null ? c.credentials().get(k) : null;
        return v == null || String.valueOf(v).isBlank() ? f : String.valueOf(v);
    }

    boolean empty(Object v) {
        return v == null || String.valueOf(v).isBlank();
    }
}