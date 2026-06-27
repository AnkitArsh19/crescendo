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
        return new App("wordpress", "WordPress", """
                WordPress is a popular open-source content management system. The Crescendo WordPress app allows you to automate blog publishing and retrieve existing posts programmatically.

                **What you can do with WordPress in Crescendo:**
                - Publish an AI-generated meeting summary directly to an internal WordPress blog
                - Cross-post your published WordPress articles to LinkedIn and Twitter automatically using a scheduled trigger
                - Sync new WordPress posts into an Airtable base for editorial tracking and SEO analysis
                - Send a celebratory Slack message to the marketing channel whenever a new post goes live

                **Actions available:**
                - Create Post — generate a new article (draft or published) with a title and HTML/text content
                - List Posts — fetch a list of recent articles, with optional search filtering

                **Who should use this:** Content managers, SEO specialists, and marketing teams automating their publishing pipeline.

                **Authentication:** WordPress credentials (Site URL, Username, and Application Password).
                """,
                "https://www.google.com/s2/favicons?domain=wordpress.org&sz=128", AuthType.APIKEY,
                List.of(),
                List.of(
                        Map.of("actionKey", "wordpress:post:create", "name", "Create Post",
                                "description", "Create a WordPress post",
                                "configSchema", List.of(
                                        Map.of("key", "title", "label", "Title", "type", "text", "required", true),
                                        Map.of("key", "content", "label", "Content", "type", "textarea", "required", true),
                                        Map.of("key", "status", "label", "Status", "type", "select", "required", false,
                                                "options", List.of(
                                                        Map.of("value", "draft", "label", "Draft"),
                                                        Map.of("value", "publish", "label", "Publish")
                                                )))),
                        Map.of("actionKey", "wordpress:post:getMany", "name", "List Posts",
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
