package com.crescendo.apps.matrix;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class MatrixApp implements AppDefinition {
    public App toApp() {
        return new App(
                "matrix",
                "Matrix",
                "Send messages to Matrix rooms",
                "https://www.google.com/s2/favicons?domain=matrix.org&sz=128",
                AuthType.APIKEY,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "send-message",
                                "name", "Send Message",
                                "description", "Send a message to a Matrix room",
                                "configSchema", List.of(
                                        Map.of("key", "roomId", "label", "Room ID", "type", "text", "required", true),
                                        Map.of("key", "message", "label", "Message", "type", "textarea", "required", true)
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "baseUrl", "label", "Homeserver URL", "type", "text", "required", true, "placeholder", "https://matrix.org"),
                Map.of("key", "accessToken", "label", "Access Token", "type", "password", "required", true)
        )).category("communication").helpUrl("https://spec.matrix.org/latest/client-server-api/");
    }
}
