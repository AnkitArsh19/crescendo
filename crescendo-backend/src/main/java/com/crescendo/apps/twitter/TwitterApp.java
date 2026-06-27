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
        return new App("twitter", "X (Twitter)", """
                X (formerly Twitter) is a global microblogging and social networking service. The Crescendo X app allows you to manage your audience and automate your social broadcasting.

                **What you can do with X in Crescendo:**
                - Post an automated tweet whenever a new product is added to your Shopify catalog
                - Cross-post a summary of a new blog article simultaneously to LinkedIn and X
                - Automatically delete old promotional tweets after a flash sale ends
                - Trigger a workflow to notify your team when a specific hashtag or brand mention goes viral

                **Actions available:**
                - Create Tweet — publish text (and media) to your feed
                - Delete Tweet — remove a specific post by its ID
                - Get User — retrieve basic profile information

                **Who should use this:** Social media managers, content creators, and brand teams automating their digital marketing.

                **Authentication:** OAuth 2.0 or API Keys (configured in the X Developer Portal).
                """,
                "https://www.google.com/s2/favicons?domain=twitter.com&sz=128", AuthType.OAUTH2,
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
