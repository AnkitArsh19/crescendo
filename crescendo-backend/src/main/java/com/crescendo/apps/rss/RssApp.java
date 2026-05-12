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
        return new App("rss", "RSS Feed", "Monitor and parse RSS/Atom feed updates",
                "/icons/rss.svg", AuthType.NONE,
                List.of(
                    Map.of("triggerKey", "new-item", "name", "New Feed Item",
                        "description", "Triggers when a new item appears in an RSS feed",
                        "configSchema", List.of(
                            Map.of("key", "feedUrl", "label", "Feed URL", "type", "text", "required", true,
                                   "placeholder", "https://blog.example.com/rss", "helpText", "RSS or Atom feed URL"),
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
                            Map.of("key", "maxItems", "label", "Max Items", "type", "text", "required", false,
                                   "placeholder", "10", "helpText", "Max items to return")))
                )
        ).credentialSchema(List.of()).category("productivity").helpUrl("");
    }
}
