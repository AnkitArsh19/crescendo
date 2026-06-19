package com.crescendo.apps.rabbitmq;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class RabbitMqApp implements AppDefinition {
    public App toApp() {
        return new App(
                "rabbitmq",
                "RabbitMQ",
                "Publish and consume messages from RabbitMQ queues or exchanges",
                "/icons/rabbitmq.svg",
                AuthType.APIKEY,
                List.of(
                        Map.of(
                                "triggerKey", "message-received",
                                "name", "Message Received",
                                "description", "Triggers when messages are consumed from a queue",
                                "configSchema", List.of(
                                        Map.of("key", "queue", "label", "Queue", "type", "text", "required", true),
                                        Map.of("key", "maxMessages", "label", "Max Messages", "type", "text", "required", false, "placeholder", "10")
                                )
                        )
                ),
                List.of(
                        Map.of(
                                "actionKey", "publish",
                                "name", "Publish",
                                "description", "Publish a message",
                                "configSchema", List.of(
                                        Map.of(
                                                "key", "mode",
                                                "label", "Mode",
                                                "type", "select",
                                                "required", false,
                                                "options", List.of(
                                                        Map.of("value", "queue", "label", "Queue"),
                                                        Map.of("value", "exchange", "label", "Exchange")
                                                )
                                        ),
                                        Map.of("key", "queue", "label", "Queue", "type", "text", "required", false),
                                        Map.of("key", "exchange", "label", "Exchange", "type", "text", "required", false),
                                        Map.of("key", "routingKey", "label", "Routing Key", "type", "text", "required", false),
                                        Map.of("key", "message", "label", "Message", "type", "textarea", "required", true)
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "host", "label", "Host", "type", "text", "required", true),
                Map.of("key", "port", "label", "Port", "type", "text", "required", false, "placeholder", "5672"),
                Map.of("key", "username", "label", "Username", "type", "text", "required", false),
                Map.of("key", "password", "label", "Password", "type", "password", "required", false),
                Map.of("key", "virtualHost", "label", "Virtual Host", "type", "text", "required", false, "placeholder", "/"),
                Map.of("key", "ssl", "label", "SSL", "type", "boolean", "required", false)
        )).category("developer").helpUrl("https://www.rabbitmq.com/client-libraries/java-api-guide");
    }
}
