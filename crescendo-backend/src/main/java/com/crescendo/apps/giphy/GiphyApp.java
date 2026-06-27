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

        return new App("giphy", "Giphy", """
                Giphy is an online database and search engine that allows users to search for and share short looping videos. The Crescendo Giphy app brings fun and expression into your automated messaging.

                **What you can do with Giphy in Crescendo:**
                - Automatically reply to new team members in Slack with a random "Welcome" GIF
                - Post a celebratory trending GIF to Discord when a sales goal is met
                - Search for context-aware GIFs to embed in automated birthday emails
                - Enrich chatbot responses with animated reactions

                **Actions available:**
                - Search GIFs — find specific animations based on a query
                - Random GIF — retrieve a completely random GIF
                - Trending GIFs — fetch currently popular animations

                **Who should use this:** Community managers, HR teams, and developers adding a touch of humor to their workflows.

                **Authentication:** API Key (create one in the Giphy Developer Portal).
                """,
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
