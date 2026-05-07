package com.crescendo.apps.toggl;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@ActionMapping(appKey = "toggl", actionKey = "start-timer")
public class TogglStartTimerHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(TogglStartTimerHandler.class);
    private static final String TOGGL_API = "https://api.track.toggl.com/api/v9";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String apiToken = creds != null ? (String) creds.get("apiKey") : null;
        if (apiToken == null || apiToken.isBlank()) return ActionResult.failure("Toggl requires 'apiKey' (API token)");

        Object workspaceId = config.get("workspaceId");
        if (workspaceId == null) return ActionResult.failure("'workspaceId' is required");

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("workspace_id", Integer.parseInt(workspaceId.toString()));
            body.put("created_with", "Crescendo");
            body.put("start", Instant.now().toString());
            body.put("duration", -1); // running timer

            if (config.containsKey("description")) body.put("description", config.get("description").toString());
            if (config.containsKey("projectId")) body.put("project_id", Integer.parseInt(config.get("projectId").toString()));

            String auth = Base64.getEncoder().encodeToString((apiToken + ":api_token").getBytes());

            String response = RestClient.create()
                    .post()
                    .uri(TOGGL_API + "/workspaces/" + workspaceId + "/time_entries")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + auth)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[toggl] Timer started successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[toggl] Start timer failed", e);
            return ActionResult.failure("Toggl start timer failed: " + e.getMessage());
        }
    }
}
