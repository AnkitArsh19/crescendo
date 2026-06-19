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
                "Instagram Graph API",
                "Create Instagram media containers and publish media",
                "/icons/instagram.svg",
                AuthType.APIKEY,
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
