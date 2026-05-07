package com.crescendo.apps.giphy;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GiphyApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("giphy", "Giphy", "Search and share GIFs from the Giphy library",
                "/icons/giphy.svg", AuthType.NONE,
                List.of(),
                List.of(
                    Map.of(
                        "actionKey", "search-gifs",
                        "name", "Search GIFs",
                        "description", "Search the Giphy library for GIFs",
                        "configSchema", List.of(
                            Map.of("key", "query", "label", "Search Query",
                                   "type", "text", "required", true,
                                   "placeholder", "funny cats",
                                   "helpText", "What GIFs are you looking for?"),
                            Map.of("key", "limit", "label", "Number of Results",
                                   "type", "number", "required", false,
                                   "placeholder", "10",
                                   "helpText", "How many GIFs to return (default: 10, max: 50)")
                        )
                    ),
                    Map.of(
                        "actionKey", "random-gif",
                        "name", "Random GIF",
                        "description", "Get a random GIF, optionally filtered by tag",
                        "configSchema", List.of(
                            Map.of("key", "tag", "label", "Tag (optional)",
                                   "type", "text", "required", false,
                                   "placeholder", "cat",
                                   "helpText", "Optional tag to filter the random GIF")
                        )
                    )
                )
        )
        .credentialSchema(List.of())
        .category("fun")
        .helpUrl("https://developers.giphy.com/dashboard/");
    }
}
