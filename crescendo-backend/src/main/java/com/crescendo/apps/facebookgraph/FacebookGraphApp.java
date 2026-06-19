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
                "Facebook Graph API",
                "Call Facebook Graph API for pages and posts",
                "/icons/facebook.svg",
                AuthType.APIKEY,
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
