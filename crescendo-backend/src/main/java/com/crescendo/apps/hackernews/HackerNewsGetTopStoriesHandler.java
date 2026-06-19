package com.crescendo.apps.hackernews;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ActionMapping(appKey = "hackernews", actionKey = "get-top-stories")
public class HackerNewsGetTopStoriesHandler implements ActionHandler {

    private final HackerNewsClient client;

    public HackerNewsGetTopStoriesHandler(HackerNewsClient client) {
        this.client = client;
    }

    @Override
public ActionResult execute(ActionContext context) {
        try {
            int limit = Math.max(1, Math.min(100, intValue(context.configuration().get("limit"), 10)));
            Long[] ids = client.getTopStoryIds();
            List<Map<String, Object>> stories = new ArrayList<>();
            if (ids != null) {
                for (int i = 0; i < Math.min(limit, ids.length); i++) {
                    stories.add(client.getItem(ids[i]));
                }
            }
            return ActionResult.success(Map.of("stories", stories, "count", stories.size()));
        } catch (Exception e) {
            return ActionResult.failure("Hacker News top stories failed: " + e.getMessage());
        }
    }

    private int intValue(Object value, int fallback) {
        if (value == null) return fallback;
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
