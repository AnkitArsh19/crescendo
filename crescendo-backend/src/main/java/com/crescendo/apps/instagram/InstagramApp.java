package com.crescendo.apps.instagram;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class InstagramApp implements AppDefinition {
    public App toApp() {
        return new App(
                "instagram",
                "Instagram Graph API", """
                The Instagram Graph API allows you to programmatically access Instagram Business and Creator accounts. The Crescendo Instagram app enables you to automate your visual social media presence.

                **What you can do with Instagram in Crescendo:**
                - Publish a new Instagram post automatically when a new product is added to your Shopify store
                - Track engagement on your latest posts and append the stats to an Airtable base
                - Alert your team in Microsoft Teams when a new high-profile comment is received
                - Automatically reply to specific DM inquiries with a standardized response

                **Triggers available:**
                - Instagram Event — start a workflow when a webhook event (like a new message or comment) is received

                **Actions available:**
                - Create Media Container — stage an image or video with a caption for publishing
                - Publish Media — take a staged media container and push it live to your Instagram feed

                **Who should use this:** Social media managers, influencers, and digital marketing teams automating cross-platform content delivery.

                **Authentication:** OAuth 2.0 (connect your linked Facebook/Instagram account).
                """,
                "https://www.google.com/s2/favicons?domain=instagram.com&sz=128",
                AuthType.OAUTH2,
                List.of(
                        Map.of(
                                "triggerKey", "instagram-event",
                                "name", "Instagram Event",
                                "description", "Triggers from Instagram/Facebook webhook delivery",
                                "configSchema", List.of()
                        )
                ),
                List.of(
                        Map.of(
                                "actionKey", "create-media-container",
                                "name", "Create Media Container",
                                "description", "Create image media container",
                                "configSchema", List.of(
                                        Map.of("key", "igUserId", "label", "Instagram User ID", "type", "text", "required", true),
                                        Map.of("key", "imageUrl", "label", "Image URL", "type", "text", "required", true),
                                        Map.of("key", "caption", "label", "Caption", "type", "textarea", "required", false)
                                )
                        ),
                        Map.of(
                                "actionKey", "publish-media",
                                "name", "Publish Media",
                                "description", "Publish a media container",
                                "configSchema", List.of(
                                        Map.of("key", "igUserId", "label", "Instagram User ID", "type", "text", "required", true),
                                        Map.of("key", "creationId", "label", "Creation ID", "type", "text", "required", true)
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "accessToken", "label", "Access Token", "type", "password", "required", true),
                Map.of("key", "graphVersion", "label", "Graph Version", "type", "text", "required", false, "placeholder", "v20.0")
        )).altAuthType(AuthType.OAUTH2).category("social").helpUrl("https://developers.facebook.com/docs/instagram-platform/");
    }
}
