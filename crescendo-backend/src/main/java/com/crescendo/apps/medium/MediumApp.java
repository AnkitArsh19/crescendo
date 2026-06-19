package com.crescendo.apps.medium;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class MediumApp implements AppDefinition {
    public App toApp() {
        return new App(
                "medium",
                "Medium",
                "Publish Medium articles with an integration token",
                "/icons/medium.svg",
                AuthType.APIKEY,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "create-post",
                                "name", "Create Post",
                                "description", "Publish a Medium post",
                                "configSchema", List.of(
                                        Map.of("key", "title", "label", "Title", "type", "text", "required", true),
                                        Map.of("key", "content", "label", "Content", "type", "textarea", "required", true),
                                        Map.of(
                                                "key", "contentFormat",
                                                "label", "Content Format",
                                                "type", "select",
                                                "required", false,
                                                "options", List.of(
                                                        Map.of("value", "markdown", "label", "Markdown"),
                                                        Map.of("value", "html", "label", "HTML")
                                                )
                                        ),
                                        Map.of(
                                                "key", "publishStatus",
                                                "label", "Publish Status",
                                                "type", "select",
                                                "required", false,
                                                "options", List.of(
                                                        Map.of("value", "draft", "label", "Draft"),
                                                        Map.of("value", "public", "label", "Public")
                                                )
                                        )
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "integrationToken", "label", "Integration Token", "type", "password", "required", true)
        )).category("social").helpUrl("https://github.com/Medium/medium-api-docs");
    }
}
