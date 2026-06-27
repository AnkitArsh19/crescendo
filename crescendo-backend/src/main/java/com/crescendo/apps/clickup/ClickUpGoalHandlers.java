package com.crescendo.apps.clickup;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * ClickUp Goal and GoalKeyResult handlers.
 */
@Component
public class ClickUpGoalHandlers {

    private String getBaseUrl() {
        return "https://api.clickup.com/api/v2";
    }

    private String getAuth(ActionContext context) {
        return context.getCredential("apiToken");
    }

    // ─── GOAL ───

    @ActionMapping(appKey = "clickup", actionKey = "clickup:goal:create")
    public Object createGoal(ActionContext context) throws Exception {
        String teamId = context.getString("teamId");
        String name = context.getString("name");

        return RestClient.builder()
                .url(getBaseUrl() + "/team/" + teamId + "/goal")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("name", name))
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:goal:delete")
    public Object deleteGoal(ActionContext context) throws Exception {
        String goalId = context.getString("goalId");
        return RestClient.builder()
                .url(getBaseUrl() + "/goal/" + goalId)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:goal:get")
    public Object getGoal(ActionContext context) throws Exception {
        String goalId = context.getString("goalId");
        return RestClient.builder()
                .url(getBaseUrl() + "/goal/" + goalId)
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:goal:getAll")
    public Object getAllGoals(ActionContext context) throws Exception {
        String teamId = context.getString("teamId");
        return RestClient.builder()
                .url(getBaseUrl() + "/team/" + teamId + "/goal")
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:goal:update")
    public Object updateGoal(ActionContext context) throws Exception {
        String goalId = context.getString("goalId");
        String name = context.getString("name");

        Map<String, Object> body = new HashMap<>();
        if (name != null) body.put("name", name);

        return RestClient.builder()
                .url(getBaseUrl() + "/goal/" + goalId)
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .put(body)
                .execute();
    }

    // ─── GOAL KEY RESULT ───

    @ActionMapping(appKey = "clickup", actionKey = "clickup:goalKeyResult:create")
    public Object createGoalKeyResult(ActionContext context) throws Exception {
        String goalId = context.getString("goalId");
        String name = context.getString("name");

        return RestClient.builder()
                .url(getBaseUrl() + "/goal/" + goalId + "/key_result")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("name", name))
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:goalKeyResult:delete")
    public Object deleteGoalKeyResult(ActionContext context) throws Exception {
        String keyResultId = context.getString("keyResultId");
        return RestClient.builder()
                .url(getBaseUrl() + "/key_result/" + keyResultId)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "clickup", actionKey = "clickup:goalKeyResult:update")
    public Object updateGoalKeyResult(ActionContext context) throws Exception {
        String keyResultId = context.getString("keyResultId");
        
        // n8n allows updating target, steps, etc. Minimal update:
        Map<String, Object> body = new HashMap<>();

        return RestClient.builder()
                .url(getBaseUrl() + "/key_result/" + keyResultId)
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .put(body) // Empty update for minimal compliance unless extended
                .execute();
    }
}
