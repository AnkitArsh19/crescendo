package com.crescendo.apps.hackernews;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;

@ActionMapping(appKey = "hackernews", actionKey = "get-user")
public class HackerNewsGetUserHandler implements ActionHandler {

    private final HackerNewsClient client;

    public HackerNewsGetUserHandler(HackerNewsClient client) {
        this.client = client;
    }

    @Override
public ActionResult execute(ActionContext context) {
        try {
            Object username = context.configuration().get("username");
            if (username == null || String.valueOf(username).isBlank()) {
                return ActionResult.failure("Hacker News username is required");
            }
            return ActionResult.success(client.getUser(String.valueOf(username)));
        } catch (Exception e) {
            return ActionResult.failure("Hacker News get user failed: " + e.getMessage());
        }
    }
}
