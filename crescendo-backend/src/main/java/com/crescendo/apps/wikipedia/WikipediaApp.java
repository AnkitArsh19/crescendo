package com.crescendo.apps.wikipedia;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class WikipediaApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("wikipedia", "Wikipedia", "Search and fetch Wikipedia article summaries",
                "https://www.google.com/s2/favicons?domain=wikipedia.org&sz=128", AuthType.NONE,
                List.of(),
                List.of(
                        Map.of("actionKey", "search", "name", "Search Articles",
                                "description", "Search Wikipedia pages",
                                "configSchema", List.of(
                                        Map.of("key", "query", "label", "Query", "type", "text", "required", true,
                                                "placeholder", "automation"),
                                        Map.of("key", "language", "label", "Language", "type", "text", "required", false,
                                                "placeholder", "en"),
                                        Map.of("key", "limit", "label", "Limit", "type", "text", "required", false,
                                                "placeholder", "10"))),
                        Map.of("actionKey", "get-summary", "name", "Get Article Summary",
                                "description", "Fetch a Wikipedia page summary",
                                "configSchema", List.of(
                                        Map.of("key", "title", "label", "Title", "type", "text", "required", true,
                                                "placeholder", "Workflow"),
                                        Map.of("key", "language", "label", "Language", "type", "text", "required", false,
                                                "placeholder", "en")))
                )
        ).credentialSchema(List.of()).category("data").helpUrl("https://www.mediawiki.org/wiki/API:REST_API");
    }
}
