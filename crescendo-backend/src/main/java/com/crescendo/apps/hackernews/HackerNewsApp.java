package com.crescendo.apps.hackernews;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class HackerNewsApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("hackernews", "Hacker News", """
                Hacker News is a social news website focusing on computer science and entrepreneurship. The Crescendo Hacker News app lets you monitor trending stories and discussions automatically.

                **What you can do with Hacker News in Crescendo:**
                - Fetch the top story every morning and post it to a specific Microsoft Teams channel
                - Trigger a workflow when a specific keyword is mentioned in new HN posts
                - Retrieve comment threads and run them through Gemini for sentiment analysis
                - Aggregate daily tech news into a single digest email

                **Actions available:**
                - Get Item — fetch a specific story, comment, or poll by ID
                - Get User — retrieve a user's profile and karma
                - Get Top Stories — pull the current highest-ranking stories

                **Who should use this:** Tech enthusiasts, founders monitoring brand mentions, and developers aggregating news feeds.

                **Authentication:** None required.
                """,
                "https://www.google.com/s2/favicons?domain=ycombinator.com&sz=128", AuthType.NONE,
                List.of(),
                List.of(
                        Map.of("actionKey", "get-top-stories", "name", "Get Top Stories",
                                "description", "Fetch top Hacker News stories",
                                "configSchema", List.of(
                                        Map.of("key", "limit", "label", "Limit", "type", "text", "required", false,
                                                "placeholder", "10", "helpText", "Number of stories to return"))),
                        Map.of("actionKey", "get-item", "name", "Get Item",
                                "description", "Fetch a story, comment, poll, or job by item ID",
                                "configSchema", List.of(
                                        Map.of("key", "itemId", "label", "Item ID", "type", "text", "required", true,
                                                "placeholder", "8863"))),
                        Map.of("actionKey", "get-user", "name", "Get User",
                                "description", "Fetch a Hacker News user profile",
                                "configSchema", List.of(
                                        Map.of("key", "username", "label", "Username", "type", "text", "required", true,
                                                "placeholder", "pg")))
                )
        ).credentialSchema(List.of()).category("data").helpUrl("https://github.com/HackerNews/API");
    }
}
