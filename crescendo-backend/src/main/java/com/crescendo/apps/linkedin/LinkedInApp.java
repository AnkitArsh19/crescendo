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
        return new App("linkedin", "LinkedIn", "Share posts and manage your professional presence",
                "/icons/linkedin.svg", AuthType.OAUTH2,
                List.of(),
                List.of(
                    Map.of(
                        "actionKey", "get-profile",
                        "name", "Get My Profile",
                        "description", "Retrieve your LinkedIn profile information (name, photo, ID)"
                    ),
                    Map.of(
                        "actionKey", "share-post",
                        "name", "Share Post",
                        "description", "Share a text post on LinkedIn",
                        "configSchema", List.of(
                            Map.of("key", "text", "label", "Post Content",
                                   "type", "textarea", "required", true,
                                   "placeholder", "Excited to share...",
                                   "helpText", "The content of your LinkedIn post"),
                            Map.of("key", "visibility", "label", "Visibility",
                                   "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "PUBLIC", "label", "Public (anyone)"),
                                       Map.of("value", "CONNECTIONS", "label", "Connections only")
                                   ),
                                   "helpText", "Who can see this post (default: Public)")
                        )
                    )
                )
        )
        .credentialSchema(List.of())
        .category("communication")
        .helpUrl("https://www.linkedin.com/developers/apps");
    }
}
