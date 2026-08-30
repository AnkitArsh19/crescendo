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
                                "triggerKey", "page-post-created",
                                "name", "New Page Post",
                                "description", "Triggers when a new post is published on your Facebook Page",
                                "configSchema", List.of(
                                        Map.of("key", "pageId", "label", "Facebook Page", "type", "dynamic_dropdown", "resourceType", "pages", "required", false)
                                )
                        ),
                        Map.of(
                                "triggerKey", "page-comment-received",
                                "name", "New Post Comment",
                                "description", "Triggers when someone comments on a Facebook Page post",
                                "configSchema", List.of(
                                        Map.of("key", "pageId", "label", "Facebook Page", "type", "dynamic_dropdown", "resourceType", "pages", "required", false)
                                )
                        ),
                        Map.of(
                                "triggerKey", "page-event",
                                "name", "Page Event (Webhook)",
                                "description", "Triggers from any Facebook Page webhook delivery",
                                "configSchema", List.of()
                        )
                ),
                List.of(
                        Map.of(
                                "actionKey", "create-page-post",
                                "name", "Create Page Post",
                                "description", "Create a Facebook Page post (text, link preview, or photo)",
                                "configSchema", List.of(
                                        Map.of("key", "pageId", "label", "Facebook Page", "type", "dynamic_dropdown", "resourceType", "pages", "required", true, "helpText", "Select the Facebook Page you want to publish to"),
                                        Map.of("key", "message", "label", "Message / Caption", "type", "textarea", "required", true, "placeholder", "Write your post caption or announcement here..."),
                                        Map.of("key", "link", "label", "Link URL (Optional)", "type", "text", "required", false, "placeholder", "https://yourwebsite.com/article", "helpText", "Attach a link preview card to the post"),
                                        Map.of("key", "imageUrl", "label", "Image / Photo (Upload or URL)", "type", "file_or_url", "required", false, "placeholder", "https://example.com/image.jpg", "helpText", "Upload a photo or enter an image URL with the message as its caption"),
                                        Map.of("key", "published", "label", "Publish Status", "type", "select", "required", false, "defaultValue", "true", "options", List.of(
                                                Map.of("label", "Published (Live immediately)", "value", "true"),
                                                Map.of("label", "Unpublished (Draft)", "value", "false")
                                        ))
                                )
                        ),
                        Map.of(
                                "actionKey", "create-page-photo",
                                "name", "Upload Page Photo",
                                "description", "Upload and publish a photo to a Facebook Page",
                                "configSchema", List.of(
                                        Map.of("key", "pageId", "label", "Facebook Page", "type", "dynamic_dropdown", "resourceType", "pages", "required", true),
                                        Map.of("key", "imageUrl", "label", "Photo (Upload or URL)", "type", "file_or_url", "required", true, "placeholder", "https://example.com/photo.png", "helpText", "Upload an image file or provide a public photo URL"),
                                        Map.of("key", "caption", "label", "Caption (Optional)", "type", "textarea", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "create-post-comment",
                                "name", "Comment on Post",
                                "description", "Add a comment to an existing Facebook Page post",
                                "configSchema", List.of(
                                        Map.of("key", "pageId", "label", "Facebook Page", "type", "dynamic_dropdown", "resourceType", "pages", "required", true, "helpText", "Select the Facebook Page"),
                                        Map.of("key", "postId", "label", "Facebook Post", "type", "dynamic_dropdown", "resourceType", "posts", "dependsOn", List.of("pageId"), "required", true, "helpText", "Select a recent post from the page or type a post ID"),
                                        Map.of("key", "message", "label", "Comment Text", "type", "textarea", "required", true, "placeholder", "Thank you for commenting!")
                                )
                        ),
                        Map.of(
                                "actionKey", "get-page-posts",
                                "name", "Get Page Posts",
                                "description", "List recent posts published by the Facebook Page",
                                "configSchema", List.of(
                                        Map.of("key", "pageId", "label", "Facebook Page", "type", "dynamic_dropdown", "resourceType", "pages", "required", true),
                                        Map.of("key", "limit", "label", "Limit", "type", "text", "required", false, "placeholder", "25")
                                )
                        ),
                        Map.of(
                                "actionKey", "get-page-insights",
                                "name", "Get Page Insights",
                                "description", "Retrieve impressions, page views, and engagement metrics",
                                "configSchema", List.of(
                                        Map.of("key", "pageId", "label", "Facebook Page", "type", "dynamic_dropdown", "resourceType", "pages", "required", true)
                                )
                        ),
                        Map.of(
                                "actionKey", "get-node",
                                "name", "Get Custom Node",
                                "description", "Query any Graph API node (e.g., a Page, User, or Post ID) for specific fields",
                                "configSchema", List.of(
                                        Map.of("key", "nodeId", "label", "Node ID", "type", "text", "required", true),
                                        Map.of("key", "fields", "label", "Fields", "type", "text", "required", false, "placeholder", "id,name")
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "accessToken", "label", "Access Token", "type", "password", "required", true),
                Map.of("key", "graphVersion", "label", "Graph Version", "type", "text", "required", false, "placeholder", "v26.0")
        )).altAuthType(AuthType.OAUTH2).category("social").helpUrl("https://developers.facebook.com/docs/graph-api/");
    }
}
