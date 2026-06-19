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
        var ratingField = Map.of("key", "rating", "label", "Rating", "type", "select", "required", false,
                "options", List.of(
                    Map.of("value", "g", "label", "G (General)"),
                    Map.of("value", "pg", "label", "PG"),
                    Map.of("value", "pg-13", "label", "PG-13"),
                    Map.of("value", "r", "label", "R")
                ), "helpText", "Content rating filter");

        return new App("giphy", "Giphy", "Search, random, and trending GIFs from Giphy",
                "https://www.google.com/s2/favicons?domain=giphy.com&sz=128", AuthType.NONE,
                List.of(),
                List.of(
                    Map.of("actionKey", "search-gifs", "name", "Search GIFs",
                        "description", "Search the Giphy library",
                        "configSchema", List.of(
                            Map.of("key", "query", "label", "Search Query", "type", "text", "required", true,
                                   "placeholder", "funny cats", "helpText", "Search keywords"),
                            Map.of("key", "limit", "label", "Results", "type", "text", "required", false,
                                   "placeholder", "10", "helpText", "Number of GIFs (max 50)"),
                            ratingField)),
                    Map.of("actionKey", "random-gif", "name", "Random GIF",
                        "description", "Get a random GIF",
                        "configSchema", List.of(
                            Map.of("key", "tag", "label", "Tag", "type", "text", "required", false,
                                   "placeholder", "cat", "helpText", "Optional tag filter"),
                            ratingField)),
                    Map.of("actionKey", "trending-gifs", "name", "Trending GIFs",
                        "description", "Get currently trending GIFs",
                        "configSchema", List.of(
                            Map.of("key", "limit", "label", "Results", "type", "text", "required", false,
                                   "placeholder", "10", "helpText", "Number of GIFs"),
                            ratingField))
                )
        ).credentialSchema(List.of()).category("fun").helpUrl("https://developers.giphy.com/dashboard/");
    }
}
