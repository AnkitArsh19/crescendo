package com.crescendo.apps.twitter;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TwitterApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
            "twitter", "X", "Post updates and engage on X (formerly Twitter)",
            "/icons/x.svg", AuthType.OAUTH2,
            List.of(),
            List.of(Map.of(
                "actionKey", "post-tweet",
                "name", "Post Update",
                "description", "Post a new update to the authenticated X account",
                "configSchema", List.of(
                    Map.of("key", "text", "label", "Post Text",
                           "type", "textarea", "required", true,
                           "placeholder", "Hello from Crescendo!",
                           "helpText", "Post content (max 280 characters)")
                )
            ))
        )
        .credentialSchema(List.of()) // OAuth — X login
        .category("communication")
        .helpUrl("https://developer.x.com/en/portal/dashboard");
    }
}
