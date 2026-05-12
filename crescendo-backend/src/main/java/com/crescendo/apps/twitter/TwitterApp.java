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
        return new App("twitter", "X (Twitter)", "Post, delete, and manage tweets on X",
                "/icons/x.svg", AuthType.OAUTH2,
                List.of(
                    Map.of("triggerKey", "new-mention", "name", "New Mention",
                        "description", "Triggers when you are mentioned in a tweet",
                        "configSchema", List.of()),
                    Map.of("triggerKey", "new-follower", "name", "New Follower",
                        "description", "Triggers when someone follows you",
                        "configSchema", List.of())
                ),
                List.of(
                    Map.of("actionKey", "post-tweet", "name", "Post Tweet",
                        "description", "Post a new tweet",
                        "configSchema", List.of(
                            Map.of("key", "text", "label", "Tweet Text", "type", "textarea", "required", true,
                                   "placeholder", "Hello from Crescendo!", "helpText", "Tweet content (max 280 chars)"))),
                    Map.of("actionKey", "delete-tweet", "name", "Delete Tweet",
                        "description", "Delete an existing tweet",
                        "configSchema", List.of(
                            Map.of("key", "tweetId", "label", "Tweet ID", "type", "text", "required", true,
                                   "placeholder", "1234567890", "helpText", "ID of the tweet to delete")))
                )
        ).credentialSchema(List.of()).category("communication").helpUrl("https://developer.x.com/en/portal/dashboard");
    }
}
