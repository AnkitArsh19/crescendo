package com.crescendo.apps.gotify;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GotifyApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("gotify", "Gotify", """
                Gotify is a simple server for sending and receiving messages in real-time. The Crescendo Gotify app allows you to push notifications directly to your self-hosted instance.

                **What you can do with Gotify in Crescendo:**
                - Send a high-priority push notification to your phone if a server goes down
                - Push a summary of daily sales to a specific Gotify channel
                - Alert your home automation system when a specific email arrives
                - Broadcast CI/CD pipeline deployment statuses to your developer team

                **Actions available:**
                - Send Message — push a title, message, and priority level to your Gotify server

                **Who should use this:** Self-hosters, homelab enthusiasts, and sysadmins requiring private, reliable push notifications.

                **Authentication:** Server URL and App Token.
                """,
                "https://www.google.com/s2/favicons?domain=gotify.net&sz=128", AuthType.APIKEY,
                List.of(),
                List.of(
                        Map.of("actionKey", "send-message", "name", "Send Message",
                                "description", "Send a Gotify push notification",
                                "configSchema", List.of(
                                        Map.of("key", "title", "label", "Title", "type", "text", "required", false),
                                        Map.of("key", "message", "label", "Message", "type", "textarea", "required", true),
                                        Map.of("key", "priority", "label", "Priority", "type", "text", "required", false,
                                                "placeholder", "5")))
                )
        ).credentialSchema(List.of(
                Map.of("key", "baseUrl", "label", "Server URL", "type", "text", "required", true,
                        "placeholder", "https://gotify.example.com"),
                Map.of("key", "appToken", "label", "Application Token", "type", "password", "required", true)
        )).category("communication").helpUrl("https://gotify.net/docs/pushmsg");
    }
}
