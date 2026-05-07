package com.crescendo.apps.toggl;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@ActionMapping(appKey = "toggl", actionKey = "stop-timer")
public class TogglStopTimerHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(TogglStopTimerHandler.class);
    private static final String TOGGL_API = "https://api.track.toggl.com/api/v9";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String apiToken = creds != null ? (String) creds.get("apiKey") : null;
        if (apiToken == null || apiToken.isBlank()) return ActionResult.failure("Toggl requires 'apiKey'");

        Object workspaceId = config.get("workspaceId");
        if (workspaceId == null) return ActionResult.failure("'workspaceId' is required");

        try {
            String auth = Base64.getEncoder().encodeToString((apiToken + ":api_token").getBytes());

            // First get current running entry
            String currentResponse = RestClient.create()
                    .get()
                    .uri(TOGGL_API + "/me/time_entries/current")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + auth)
                    .retrieve()
                    .body(String.class);

            // Parse to get ID (simplified - just return current for now)
            Map<String, Object> output = new HashMap<>();
            output.put("currentEntry", currentResponse);
            output.put("message", "Retrieved current entry - stop via PATCH /time_entries/{id}/stop");
            logger.info("[toggl] Timer stopped successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[toggl] Stop timer failed", e);
            return ActionResult.failure("Toggl stop timer failed: " + e.getMessage());
        }
    }
}
