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
        return new App("wikipedia", "Wikipedia", """
                Wikipedia is a free, multilingual open-collaborative online encyclopedia. The Crescendo Wikipedia app allows you to automatically fetch article summaries and knowledge data.

                **What you can do with Wikipedia in Crescendo:**
                - Automatically reply to specific Discord questions with the first paragraph of a relevant Wikipedia article
                - Append Wikipedia context summaries to daily news briefs generated for your team
                - Fetch metadata and URLs for historical figures mentioned in an incoming RSS feed
                - Build a custom chatbot in Slack that queries Wikipedia when you type `/wiki [topic]`

                **Actions available:**
                - Get Article Summary — fetch the introductory text, URL, and thumbnail image for a specific search term

                **Who should use this:** Bot developers, researchers, and community managers automating knowledge retrieval.

                **Authentication:** None required.
                """,
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
