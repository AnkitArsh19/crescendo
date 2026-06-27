package com.crescendo.apps.facebookgraph;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class FacebookGraphApp implements AppDefinition {
    public App toApp() {
        return new App(
                "facebook-graph",
                "Facebook Graph API", """
                The Graph API is the primary way to get data into and out of the Facebook platform. The Crescendo Facebook Graph app lets you automate Page interactions and post content programmatically.

                **What you can do with Facebook in Crescendo:**
                - Automatically post a new blog article to your Facebook Page when an RSS feed updates
                - Track page engagement metrics and log them into Google Sheets
                - Notify your team in Slack when a user mentions your Page
                - Sync Facebook Lead Ads directly into HubSpot

                **Triggers available:**
                - Page Event — start a workflow when a webhook event (like a new comment or message) is received

                **Actions available:**
                - Create Page Post — publish text, links, or images to a Facebook Page
                - Get Node — query any Graph API node (e.g., a Page, User, or Post ID) for specific fields

                **Who should use this:** Social media managers, community moderators, and digital marketers.

                **Authentication:** OAuth 2.0 (connect your Facebook account) or Page Access Token.
                """,
                "https://www.google.com/s2/favicons?domain=facebook.com&sz=128",
                AuthType.OAUTH2,
                List.of(
                        Map.of(
                                "triggerKey", "page-event",
                                "name", "Page Event",
                                "description", "Triggers from Facebook webhook delivery",
                                "configSchema", List.of()
                        )
                ),
                List.of(
                        Map.of(
                                "actionKey", "get-node",
                                "name", "Get Node",
                                "description", "Get a Graph API node",
                                "configSchema", List.of(
                                        Map.of("key", "nodeId", "label", "Node ID", "type", "text", "required", true),
                                        Map.of("key", "fields", "label", "Fields", "type", "text", "required", false, "placeholder", "id,name")
                                )
                        ),
                        Map.of(
                                "actionKey", "create-page-post",
                                "name", "Create Page Post",
                                "description", "Create a Facebook Page post",
                                "configSchema", List.of(
                                        Map.of("key", "pageId", "label", "Page ID", "type", "text", "required", true),
                                        Map.of("key", "message", "label", "Message", "type", "textarea", "required", true)
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "accessToken", "label", "Access Token", "type", "password", "required", true),
                Map.of("key", "graphVersion", "label", "Graph Version", "type", "text", "required", false, "placeholder", "v20.0")
        )).altAuthType(AuthType.OAUTH2).category("social").helpUrl("https://developers.facebook.com/docs/graph-api/");
    }
}
