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
        return new App("hackernews", "Hacker News", "Fetch stories, users, and comments from Hacker News",
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
