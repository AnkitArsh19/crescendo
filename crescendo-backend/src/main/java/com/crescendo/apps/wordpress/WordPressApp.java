package com.crescendo.apps.wordpress;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class WordPressApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("wordpress", "WordPress", "Create and fetch WordPress posts using application passwords",
                "https://www.google.com/s2/favicons?domain=wordpress.org&sz=128", AuthType.APIKEY,
                List.of(),
                List.of(
                        Map.of("actionKey", "create-post", "name", "Create Post",
                                "description", "Create a WordPress post",
                                "configSchema", List.of(
                                        Map.of("key", "title", "label", "Title", "type", "text", "required", true),
                                        Map.of("key", "content", "label", "Content", "type", "textarea", "required", true),
                                        Map.of("key", "status", "label", "Status", "type", "select", "required", false,
                                                "options", List.of(
                                                        Map.of("value", "draft", "label", "Draft"),
                                                        Map.of("value", "publish", "label", "Publish")
                                                )))),
                        Map.of("actionKey", "list-posts", "name", "List Posts",
                                "description", "List recent WordPress posts",
                                "configSchema", List.of(
                                        Map.of("key", "perPage", "label", "Per Page", "type", "text", "required", false,
                                                "placeholder", "10"),
                                        Map.of("key", "search", "label", "Search", "type", "text", "required", false)))
                )
        ).credentialSchema(List.of(
                Map.of("key", "siteUrl", "label", "Site URL", "type", "text", "required", true,
                        "placeholder", "https://example.com"),
                Map.of("key", "username", "label", "Username", "type", "text", "required", true),
                Map.of("key", "applicationPassword", "label", "Application Password", "type", "password", "required", true)
        )).category("cms").helpUrl("https://developer.wordpress.org/rest-api/");
    }
}
