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
                "Matrix", """
                Matrix is an open standard for interoperable, decentralized, real-time communication. The Crescendo Matrix app allows you to send automated messages to specific Matrix rooms securely.
                
                **What you can do with Matrix in Crescendo:**
                - Broadcast announcements to public Matrix rooms
                - Create dedicated incident response rooms instantly
                - Moderate rooms by automatically deleting specific messages
                
                **Actions available:**
                - Send Message — send a message to a Matrix room
                - Delete Message — redact a message in a Matrix room
                - Create Room — create a new Matrix room
                
                **Who should use this:** Security teams, open-source communities, and organizations adopting decentralized communications.
                
                **Authentication:** Requires a Homeserver URL and an Access Token.
                """,
                "https://www.google.com/s2/favicons?domain=matrix.org&sz=128",
                AuthType.APIKEY,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "sendMessage",
                                "name", "Send Message",
                                "description", "Send a message to a Matrix room",
                                "configSchema", List.of(
                                        Map.of("key", "roomId", "label", "Room ID", "type", "text", "required", true),
                                        Map.of("key", "message", "label", "Message", "type", "textarea", "required", true)
                                )
                        ),
                        Map.of(
                                "actionKey", "deleteMessage",
                                "name", "Delete Message",
                                "description", "Redact (delete) a message in a Matrix room",
                                "configSchema", List.of(
                                        Map.of("key", "roomId", "label", "Room ID", "type", "text", "required", true),
                                        Map.of("key", "eventId", "label", "Event ID", "type", "text", "required", true)
                                )
                        ),
                        Map.of(
                                "actionKey", "createRoom",
                                "name", "Create Room",
                                "description", "Create a new Matrix room",
                                "configSchema", List.of(
                                        Map.of("key", "name", "label", "Room Name", "type", "text", "required", true),
                                        Map.of("key", "visibility", "label", "Visibility", "type", "dropdown", "required", false,
                                               "options", List.of(Map.of("value", "private", "label", "Private"), Map.of("value", "public", "label", "Public")))
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "baseUrl", "label", "Homeserver URL", "type", "text", "required", true, "placeholder", "https://matrix.org"),
                Map.of("key", "accessToken", "label", "Access Token", "type", "password", "required", true)
        )).category("communication").helpUrl("https://spec.matrix.org/latest/client-server-api/");
    }
}
