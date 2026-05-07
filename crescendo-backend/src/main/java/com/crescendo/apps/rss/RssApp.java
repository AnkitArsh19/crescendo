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
                List.of(Map.of(
                    "triggerKey", "new-item",
                    "name", "New Feed Item",
                    "description", "Triggers when a new item appears in the RSS feed"
                )),
                List.of(Map.of(
                    "actionKey", "parse-feed",
                    "name", "Parse Feed",
                    "description", "Fetch and parse an RSS/Atom feed",
                    "configSchema", Map.of(
                        "feedUrl", "string (required) — RSS or Atom feed URL"
                    )
                ))
        )
        .credentialSchema(List.of())
        .category("productivity")
        .helpUrl("");
    }
}
