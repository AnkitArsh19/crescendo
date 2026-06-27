package com.crescendo.apps.hackernews;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class HackerNewsHandlers {

    private static final String API_BASE = "https://hacker-news.firebaseio.com/v0";

    @ActionMapping(appKey = "hackernews", actionKey = "get-item")
    public Object getItem(ActionContext context) throws Exception {
        String itemId = context.configuration().get("itemId") != null ? context.configuration().get("itemId").toString() : "";
        if (itemId.isBlank()) {
            return ActionResult.failure("Hacker News item ID is required");
        }
        
        try {
            String response = RestClient.create(API_BASE)
                    .get()
                    .uri("/item/{id}.json", itemId)
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", response));
        } catch (Exception e) {
            return ActionResult.failure("Failed to fetch Hacker News item: " + e.getMessage());
        }
    }

    @ActionMapping(appKey = "hackernews", actionKey = "get-top-stories")
    public Object getTopStories(ActionContext context) throws Exception {
        int limit = 10;
        if (context.configuration().containsKey("limit")) {
            try {
                limit = Integer.parseInt(String.valueOf(context.configuration().get("limit")));
            } catch (NumberFormatException ignored) {}
        }

        try {
            List<?> ids = RestClient.create(API_BASE)
                    .get()
                    .uri("/topstories.json")
                    .retrieve()
                    .body(List.class);
            
            if (ids == null) return ActionResult.success(Map.of("data", List.of()));
            
            List<Object> stories = new ArrayList<>();
            for (int i = 0; i < Math.min(limit, ids.size()); i++) {
                Object id = ids.get(i);
                String itemResponse = RestClient.create(API_BASE)
                        .get()
                        .uri("/item/{id}.json", id)
                        .retrieve()
                        .body(String.class);
                stories.add(itemResponse);
            }
            return ActionResult.success(Map.of("data", stories));
        } catch (Exception e) {
            return ActionResult.failure("Failed to fetch Hacker News top stories: " + e.getMessage());
        }
    }

    @ActionMapping(appKey = "hackernews", actionKey = "get-user")
    public Object getUser(ActionContext context) throws Exception {
        String username = context.configuration().get("username") != null ? context.configuration().get("username").toString() : "";
        if (username.isBlank()) {
            return ActionResult.failure("Hacker News username is required");
        }

        try {
            String response = RestClient.create(API_BASE)
                    .get()
                    .uri("/user/{id}.json", username)
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", response));
        } catch (Exception e) {
            return ActionResult.failure("Failed to fetch Hacker News user: " + e.getMessage());
        }
    }
}
