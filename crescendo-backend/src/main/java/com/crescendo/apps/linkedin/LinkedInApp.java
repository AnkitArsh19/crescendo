package com.crescendo.apps.linkedin;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class LinkedInApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("linkedin", "LinkedIn", """
                LinkedIn is the world's largest professional network. The Crescendo LinkedIn app allows you to manage your professional presence and automate content sharing.

                **What you can do with LinkedIn in Crescendo:**
                - Automatically share a new blog post to your personal LinkedIn feed when your RSS feed updates
                - Announce new product launches simultaneously across Twitter, Facebook, and LinkedIn
                - Generate a weekly summary of your LinkedIn post engagement metrics and log them to Notion
                - Trigger an automated direct message when a new connection is added (via webhook)

                **Actions available:**
                - Create Post — publish a text, article, or image post to your personal profile or company page
                - Get Profile — retrieve basic information about the authenticated user

                **Who should use this:** B2B marketers, thought leaders, and recruiters automating their outreach and content distribution.

                **Authentication:** OAuth 2.0 (connect your LinkedIn account).
                """,
                "https://www.google.com/s2/favicons?domain=linkedin.com&sz=128", AuthType.OAUTH2,
                List.of(
                    Map.of("triggerKey", "new-follower", "name", "New Follower",
                        "description", "Triggers when a new person follows you",
                        "configSchema", List.of()),
                    Map.of("triggerKey", "profile-viewed", "name", "Profile Viewed",
                        "description", "Triggers when someone views your profile",
                        "configSchema", List.of())
                ),
                List.of(
                    Map.of("actionKey", "share-post", "name", "Share Post",
                        "description", "Share a text post on LinkedIn",
                        "configSchema", List.of(
                            Map.of("key", "text", "label", "Post Content", "type", "textarea", "required", true,
                                   "placeholder", "Excited to share...", "helpText", "Post content"),
                            Map.of("key", "visibility", "label", "Visibility", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "PUBLIC", "label", "Public"),
                                       Map.of("value", "CONNECTIONS", "label", "Connections only")
                                   ), "helpText", "Who can see this post"))),
                    Map.of("actionKey", "share-link", "name", "Share Link/Article",
                        "description", "Share a post with a link attachment",
                        "configSchema", List.of(
                            Map.of("key", "text", "label", "Post Text", "type", "textarea", "required", true, "helpText", "Accompanying text"),
                            Map.of("key", "linkUrl", "label", "Link URL", "type", "text", "required", true,
                                   "placeholder", "https://blog.example.com/article", "helpText", "URL to share"))),
                    Map.of("actionKey", "get-profile", "name", "Get My Profile",
                        "description", "Retrieve your LinkedIn profile information",
                        "configSchema", List.of())
                )
        ).credentialSchema(List.of()).category("communication").helpUrl("https://www.linkedin.com/developers/apps");
    }
}
