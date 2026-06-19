package com.crescendo.apps.mattermost;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class MattermostApp implements AppDefinition {
    public App toApp() {
        return new App(
                "mattermost",
                "Mattermost",
                "Post messages to Mattermost channels",
                "/icons/mattermost.svg",
                AuthType.APIKEY,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "create-post",
                                "name", "Create Post",
                                "description", "Post a message",
                                "configSchema", List.of(
                                        Map.of("key", "channelId", "label", "Channel ID", "type", "text", "required", true),
                                        Map.of("key", "message", "label", "Message", "type", "textarea", "required", true)
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "baseUrl", "label", "Base URL", "type", "text", "required", true),
                Map.of("key", "accessToken", "label", "Personal Access Token", "type", "password", "required", true)
        )).category("communication").helpUrl("https://api.mattermost.com/");
    }
}
