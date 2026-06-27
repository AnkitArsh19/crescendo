package com.crescendo.apps.rss;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class RssApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("rss", "RSS Feed", """
                RSS (Really Simple Syndication) is a web feed that allows users and applications to access updates to websites in a standardized format. The Crescendo RSS app lets you monitor your favorite blogs, news sites, and podcasts.

                **What you can do with RSS in Crescendo:**
                - Trigger workflows whenever a new article is published on a blog
                - Post new Hacker News or TechCrunch stories to a Slack channel
                - Aggregate content from multiple sources into a Notion database
                - Generate AI summaries of long articles using Gemini or OpenAI

                **Actions available:**
                - Parse Feed — fetch and parse the latest items from an RSS/Atom URL

                **Who should use this:** Content curators, social media managers, and teams monitoring industry news or competitor updates.

                **Authentication:** None required.
                """,
                "/icons/rss.svg", AuthType.NONE,
                List.of(
                    Map.of("triggerKey", "new-item", "name", "New Feed Item",
                        "description", "Triggers when a new item appears in an RSS feed",
                        "configSchema", List.of(
                            Map.of("key", "feedUrl", "label", "Feed URL", "type", "text", "required", true,
                                   "placeholder", "https://blog.example.com/rss", "helpText", "RSS or Atom feed URL"),
                            Map.of("key", "customFields", "label", "Custom Fields", "type", "text", "required", false,
                                   "helpText", "Comma-separated list of custom XML fields to extract"),
                            Map.of("key", "ignoreSSL", "label", "Ignore SSL Issues", "type", "boolean", "required", false),
                            Map.of("key", "includeFullDescription", "label", "Full Description", "type", "dropdown", "required", false,
                                   "options", List.of(
                                       Map.of("value", "true", "label", "Yes"),
                                       Map.of("value", "false", "label", "No")
                                   ), "helpText", "Include full article content")))
                ),
                List.of(
                    Map.of("actionKey", "parse-feed", "name", "Parse Feed",
                        "description", "Fetch and parse an RSS/Atom feed",
                        "configSchema", List.of(
                            Map.of("key", "feedUrl", "label", "Feed URL", "type", "text", "required", true,
                                   "placeholder", "https://blog.example.com/rss", "helpText", "RSS or Atom feed URL"),
                            Map.of("key", "customFields", "label", "Custom Fields", "type", "text", "required", false,
                                   "helpText", "Comma-separated list of custom XML fields to extract"),
                            Map.of("key", "ignoreSSL", "label", "Ignore SSL Issues", "type", "boolean", "required", false),
                            Map.of("key", "maxItems", "label", "Max Items", "type", "text", "required", false,
                                   "placeholder", "10", "helpText", "Max items to return")))
                )
        ).credentialSchema(List.of()).category("productivity").helpUrl("");
    }
}
