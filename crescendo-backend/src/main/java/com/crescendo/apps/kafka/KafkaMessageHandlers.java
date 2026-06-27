package com.crescendo.apps.kafka;

import com.crescendo.execution.action.*;
import tools.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class KafkaMessageHandlers {
    private final ObjectMapper mapper;

    public KafkaMessageHandlers(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @ActionMapping(appKey = "kafka", actionKey = "publish")
    public ActionResult publishMessage(ActionContext c) {
        try (Producer<String, String> producer = new KafkaProducer<>(props(c))) {
            boolean sendInputData = "true".equalsIgnoreCase(String.valueOf(c.configuration().getOrDefault("sendInputData", "true")));
            String payload;
            if (sendInputData) {
                payload = mapper.writeValueAsString(c.input());
            } else {
                payload = String.valueOf(c.configuration().getOrDefault("message", ""));
            }

            ProducerRecord<String, String> rec = new ProducerRecord<>(String.valueOf(c.configuration().get("topic")),
                    empty(c.configuration().get("key")) ? null : String.valueOf(c.configuration().get("key")),
                    payload);
                    
            String eventName = String.valueOf(c.configuration().getOrDefault("eventName", ""));
            if (!eventName.isBlank()) {
                rec.headers().add(new RecordHeader("eventName", eventName.getBytes(StandardCharsets.UTF_8)));
            }

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
        
        if ("true".equalsIgnoreCase(String.valueOf(c.configuration().get("acks")))) {
            p.put(ProducerConfig.ACKS_CONFIG, "all");
        }
        if (c.configuration().containsKey("timeout")) {
            p.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, String.valueOf(c.configuration().get("timeout")));
        }
        if ("true".equalsIgnoreCase(String.valueOf(c.configuration().get("compression")))) {
            p.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        }

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
        Map<String, Object> out = new LinkedHashMap<>();
        
        Object h = c.configuration().get("headers");
        if (h != null) {
            if (h instanceof Map<?, ?> m) {
                m.forEach((k, v) -> out.put(String.valueOf(k), v));
            } else {
                try {
                    Map<String, Object> parsed = mapper.readValue(String.valueOf(h), Map.class);
                    if (parsed != null) out.putAll(parsed);
                } catch(Exception e){}
            }
        }
        
        Object hui = c.configuration().get("headersUi");
        if (hui instanceof Map<?, ?> m && m.containsKey("headerValues")) {
            Object vals = m.get("headerValues");
            if (vals instanceof List<?> l) {
                for (Object item : l) {
                    if (item instanceof Map<?,?> im) {
                        out.put(String.valueOf(im.get("key")), im.get("value"));
                    }
                }
            }
        }

        return out;
    }

    String cred(ActionContext c, String k, String f) {
        Object v = c.credentials() != null ? c.credentials().get(k) : null;
        return v == null || String.valueOf(v).isBlank() ? f : String.valueOf(v);
    }

    boolean empty(Object v) {
        return v == null || String.valueOf(v).isBlank();
    }
}
