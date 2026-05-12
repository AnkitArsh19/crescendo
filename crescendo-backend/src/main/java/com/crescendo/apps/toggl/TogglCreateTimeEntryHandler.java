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

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates a completed time entry in Toggl Track via POST /api/v9/workspaces/{wid}/time_entries.
 */
@ActionMapping(appKey = "toggl", actionKey = "create-time-entry")
public class TogglCreateTimeEntryHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(TogglCreateTimeEntryHandler.class);
    private static final String TOGGL_API = "https://api.track.toggl.com/api/v9/workspaces/";
    private final RestClient restClient = RestClient.create();

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String apiToken = creds != null ? (String) creds.get("apiToken") : null;
        if (apiToken == null) apiToken = creds != null ? (String) creds.get("apiKey") : null;
        if (apiToken == null || apiToken.isBlank()) {
            return ActionResult.failure("Toggl requires an 'apiToken'");
        }

        String workspaceId = str(config, "workspaceId");
        String description = str(config, "description");
        String durationStr = str(config, "duration");
        String startTime = str(config, "startTime");
        if (workspaceId == null) return ActionResult.failure("'workspaceId' is required");
        if (description == null) return ActionResult.failure("'description' is required");

        logger.info("[toggl] Creating time entry: desc='{}', workspace='{}'", description, workspaceId);

        try {
            String basicAuth = Base64.getEncoder().encodeToString((apiToken + ":api_token").getBytes());

            Map<String, Object> body = new HashMap<>();
            body.put("description", description);
            body.put("workspace_id", Integer.parseInt(workspaceId));
            body.put("created_with", "crescendo");

            if (durationStr != null) {
                body.put("duration", Integer.parseInt(durationStr));
            } else {
                body.put("duration", -1); // running timer
            }

            if (startTime != null) {
                body.put("start", startTime);
            } else {
                body.put("start", java.time.OffsetDateTime.now().toString());
            }

            String projectId = str(config, "projectId");
            if (projectId != null) body.put("project_id", Integer.parseInt(projectId));

            String tags = str(config, "tags");
            if (tags != null && !tags.isBlank()) {
                body.put("tags", java.util.Arrays.asList(tags.split(",")));
            }

            Map<String, Object> response = restClient.post()
                    .uri(TOGGL_API + workspaceId + "/time_entries")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "toggl");
            output.put("action", "create-time-entry");
            output.put("entryId", response != null ? response.get("id") : null);
            output.put("description", description);
            output.put("duration", durationStr);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[toggl] Create time entry failed: {}", e.getMessage());
            return ActionResult.failure("Toggl create-time-entry failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : null;
    }
}
