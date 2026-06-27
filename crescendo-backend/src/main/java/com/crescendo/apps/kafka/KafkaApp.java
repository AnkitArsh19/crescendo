package com.crescendo.apps.kafka;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for Kafka.
 */
@Component
public class KafkaApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "kafka",
                "Kafka",
                """
                Apache Kafka is an open-source distributed event streaming platform used by thousands of companies for high-performance data pipelines, streaming analytics, data integration, and mission-critical applications.
                
                This integration provides operations for:
                - **Send Message**: Send a message to a Kafka topic
                
                Authenticate using Kafka broker credentials.
                """,
                "https://www.google.com/s2/favicons?domain=kafka.apache.org&sz=128", // Generic icon
                AuthType.APIKEY, // Uses custom credentials map
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "publish",
                                "name", "Send Message",
                                "description", "Send a message to a Kafka topic",
                                "configSchema", List.of(
                                        Map.of("key", "topic", "label", "Topic", "type", "text", "required", true),
                                        Map.of("key", "message", "label", "Message", "type", "text", "required", true),
                                        Map.of("key", "sendInputData", "label", "Send Input Data as JSON", "type", "boolean", "default", false),
                                        Map.of("key", "useKey", "label", "Use Key", "type", "boolean", "default", false),
                                        Map.of("key", "key", "label", "Key", "type", "text"),
                                        Map.of("key", "useSchemaRegistry", "label", "Use Schema Registry", "type", "boolean", "default", false),
                                        Map.of("key", "schemaRegistryUrl", "label", "Schema Registry URL", "type", "text"),
                                        Map.of("key", "eventName", "label", "Event Name", "type", "text"),
                                        Map.of("key", "headers", "label", "Headers (JSON)", "type", "json"),
                                        Map.of("key", "options", "label", "Options", "type", "json")
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "clientId", "label", "Client ID", "type", "text", "default", "crescendo-app"),
                Map.of("key", "brokers", "label", "Brokers", "type", "text", "required", true),
                Map.of("key", "ssl", "label", "SSL", "type", "boolean", "default", true),
                Map.of("key", "authentication", "label", "Authentication", "type", "boolean", "default", false),
                Map.of("key", "username", "label", "Username", "type", "text"),
                Map.of("key", "password", "label", "Password", "type", "password"),
                Map.of("key", "saslMechanism", "label", "SASL Mechanism", "type", "text", "default", "plain")
        )).category("messaging");
    }
}
