package com.crescendo.apps.rabbitmq;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for RabbitMQ.
 */
@Component
public class RabbitMqApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "rabbitmq",
                "RabbitMQ",
                """
                RabbitMQ is an open-source message-broker software that originally implemented the Advanced Message Queuing Protocol (AMQP).
                
                This integration provides operations for:
                - **Send Message**: Send a message to a RabbitMQ queue or exchange
                
                Authenticate using RabbitMQ broker credentials.
                """,
                "https://www.google.com/s2/favicons?domain=rabbitmq.com&sz=128", // Generic icon
                AuthType.APIKEY, // Uses custom credentials map
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "publish",
                                "name", "Send Message",
                                "description", "Send a message to a RabbitMQ queue or exchange",
                                "configSchema", List.of(
                                        Map.of("key", "mode", "label", "Mode", "type", "text", "required", true, "default", "queue"),
                                        Map.of("key", "queue", "label", "Queue / Topic", "type", "text"),
                                        Map.of("key", "exchange", "label", "Exchange", "type", "text"),
                                        Map.of("key", "exchangeType", "label", "Exchange Type", "type", "text", "default", "fanout"),
                                        Map.of("key", "routingKey", "label", "Routing Key", "type", "text"),
                                        Map.of("key", "message", "label", "Message", "type", "text", "required", true),
                                        Map.of("key", "sendInputData", "label", "Send Input Data as JSON", "type", "boolean", "default", false),
                                        Map.of("key", "options", "label", "Options", "type", "json")
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "hostname", "label", "Hostname", "type", "text", "required", true),
                Map.of("key", "port", "label", "Port", "type", "number", "default", 5672),
                Map.of("key", "username", "label", "Username", "type", "text"),
                Map.of("key", "password", "label", "Password", "type", "password"),
                Map.of("key", "vhost", "label", "Vhost", "type", "text", "default", "/"),
                Map.of("key", "ssl", "label", "SSL", "type", "boolean", "default", false),
                Map.of("key", "passwordless", "label", "Passwordless", "type", "boolean", "default", true),
                Map.of("key", "ca", "label", "CA Certificates", "type", "text"),
                Map.of("key", "cert", "label", "Client Certificate", "type", "text"),
                Map.of("key", "key", "label", "Client Key", "type", "password"),
                Map.of("key", "passphrase", "label", "Passphrase", "type", "password")
        )).category("messaging");
    }
}
