package com.crescendo.apps.mqtt;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for MQTT.
 */
@Component
public class MqttApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "mqtt",
                "MQTT",
                """
                MQTT is a machine-to-machine (M2M)/"Internet of Things" connectivity protocol.
                
                This integration provides operations for:
                - **Publish**: Publish a message to an MQTT topic
                
                Authenticate using MQTT broker credentials.
                """,
                "https://www.google.com/s2/favicons?domain=mqtt.org&sz=128", // Generic icon
                AuthType.APIKEY, // Uses custom credentials map
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "publish",
                                "name", "Publish Message",
                                "description", "Push a message to an MQTT topic",
                                "configSchema", List.of(
                                        Map.of("key", "topic", "label", "Topic", "type", "text", "required", true),
                                        Map.of("key", "message", "label", "Message", "type", "text", "required", true),
                                        Map.of("key", "qos", "label", "QoS", "type", "number", "default", 0),
                                        Map.of("key", "retain", "label", "Retain", "type", "boolean", "default", false)
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "protocol", "label", "Protocol", "type", "text", "default", "mqtt"),
                Map.of("key", "host", "label", "Host", "type", "text", "required", true),
                Map.of("key", "port", "label", "Port", "type", "number", "default", 1883),
                Map.of("key", "username", "label", "Username", "type", "text"),
                Map.of("key", "password", "label", "Password", "type", "password"),
                Map.of("key", "clean", "label", "Clean Session", "type", "boolean", "default", true),
                Map.of("key", "clientId", "label", "Client ID", "type", "text"),
                Map.of("key", "ssl", "label", "SSL", "type", "boolean", "default", false),
                Map.of("key", "passwordless", "label", "Passwordless (SSL)", "type", "boolean", "default", true),
                Map.of("key", "ca", "label", "CA Certificates", "type", "text"),
                Map.of("key", "rejectUnauthorized", "label", "Reject Unauthorized Certificate", "type", "boolean", "default", false),
                Map.of("key", "cert", "label", "Client Certificate", "type", "text"),
                Map.of("key", "key", "label", "Client Key", "type", "password")
        )).category("messaging");
    }
}
