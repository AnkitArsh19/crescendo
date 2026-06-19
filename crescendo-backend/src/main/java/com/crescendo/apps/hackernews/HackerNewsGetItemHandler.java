package com.crescendo.apps.hackernews;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;

@ActionMapping(appKey = "hackernews", actionKey = "get-item")
public class HackerNewsGetItemHandler implements ActionHandler {

    private final HackerNewsClient client;

    public HackerNewsGetItemHandler(HackerNewsClient client) {
        this.client = client;
    }

    @Override
public ActionResult execute(ActionContext context) {
        try {
            Object itemId = context.configuration().get("itemId");
            if (itemId == null || String.valueOf(itemId).isBlank()) {
                return ActionResult.failure("Hacker News itemId is required");
            }
            return ActionResult.success(client.getItem(Long.parseLong(String.valueOf(itemId))));
        } catch (Exception e) {
            return ActionResult.failure("Hacker News get item failed: " + e.getMessage());
        }
    }
}
