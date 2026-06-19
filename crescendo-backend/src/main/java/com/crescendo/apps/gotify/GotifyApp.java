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
        return new App("gotify", "Gotify", "Send notifications through a self-hosted Gotify server",
                "/icons/gotify.svg", AuthType.APIKEY,
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
