package com.crescendo.apps.kafka;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class KafkaApp implements AppDefinition {
    public App toApp() {
        return new App(
                "kafka",
                "Kafka",
                "Publish and consume messages from Kafka topics",
                "/icons/kafka.svg",
                AuthType.APIKEY,
                List.of(
                        Map.of(
                                "triggerKey", "message-received",
                                "name", "Message Received",
                                "description", "Triggers when Kafka records are consumed",
                                "configSchema", List.of(
                                        Map.of("key", "topic", "label", "Topic", "type", "text", "required", true),
                                        Map.of("key", "groupId", "label", "Consumer Group ID", "type", "text", "required", true, "placeholder", "crescendo-workflow"),
                                        Map.of("key", "maxMessages", "label", "Max Messages", "type", "text", "required", false, "placeholder", "10"),
                                        Map.of("key", "pollSeconds", "label", "Poll Seconds", "type", "text", "required", false, "placeholder", "5")
                                )
                        )
                ),
                List.of(
                        Map.of(
                                "actionKey", "publish",
                                "name", "Publish",
                                "description", "Publish to a Kafka topic",
                                "configSchema", List.of(
                                        Map.of("key", "topic", "label", "Topic", "type", "text", "required", true),
                                        Map.of("key", "message", "label", "Message", "type", "textarea", "required", true),
                                        Map.of("key", "key", "label", "Message Key", "type", "text", "required", false),
                                        Map.of("key", "headers", "label", "Headers (JSON)", "type", "json", "required", false)
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "brokers", "label", "Brokers CSV", "type", "text", "required", true, "placeholder", "localhost:9092"),
                Map.of("key", "clientId", "label", "Client ID", "type", "text", "required", false),
                Map.of("key", "ssl", "label", "SSL", "type", "boolean", "required", false),
                Map.of("key", "username", "label", "Username", "type", "text", "required", false),
                Map.of("key", "password", "label", "Password", "type", "password", "required", false),
                Map.of("key", "saslMechanism", "label", "SASL Mechanism", "type", "text", "required", false, "placeholder", "PLAIN")
        )).category("developer").helpUrl("https://kafka.apache.org/documentation/");
    }
}
