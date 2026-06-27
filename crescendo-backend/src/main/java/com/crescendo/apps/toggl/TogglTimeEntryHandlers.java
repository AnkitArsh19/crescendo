package com.crescendo.apps.toggl;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Toggl Time Entry handlers.
 * Operations: create, getCurrent, startTimer, stopTimer
 */
@Component
public class TogglTimeEntryHandlers {

    private static final String TOGGL_API = "https://api.track.toggl.com/api/v9";
    private final RestClient restClient = RestClient.create();

    private String getAuth(ActionContext context) {
        Map<String, Object> creds = context.credentials();
        String apiToken = creds != null ? (String) creds.get("apiToken") : null;
        if (apiToken == null) apiToken = creds != null ? (String) creds.get("apiKey") : null;
        if (apiToken == null || apiToken.isBlank()) {
            return null;
        }
        return "Basic " + Base64.getEncoder().encodeToString((apiToken + ":api_token").getBytes());
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m != null ? m.get(k) : null;
        return v != null ? v.toString() : null;
    }

    @ActionMapping(appKey = "toggl", actionKey = "createTimeEntry")
    @SuppressWarnings("unchecked")
    public ActionResult createTimeEntry(ActionContext context) {
        String auth = getAuth(context);
        if (auth == null) return ActionResult.failure("Toggl requires an 'apiToken' or 'apiKey'");

        Map<String, Object> config = context.configuration();
        String workspaceId = str(config, "workspaceId");
        String description = str(config, "description");
        String durationStr = str(config, "duration");
        String startTime = str(config, "startTime");

        if (workspaceId == null) return ActionResult.failure("'workspaceId' is required");
        if (description == null) return ActionResult.failure("'description' is required");

        try {
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
                    .uri(TOGGL_API + "/workspaces/" + workspaceId + "/time_entries")
                    .header(HttpHeaders.AUTHORIZATION, auth)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(Map.of("data", response));
        } catch (Exception e) {
            return ActionResult.failure("Toggl createTimeEntry failed: " + e.getMessage());
        }
    }

    @ActionMapping(appKey = "toggl", actionKey = "getCurrentTimeEntry")
    @SuppressWarnings("unchecked")
    public ActionResult getCurrentTimeEntry(ActionContext context) {
        String auth = getAuth(context);
        if (auth == null) return ActionResult.failure("Toggl requires an 'apiToken' or 'apiKey'");

        try {
            Map<String, Object> response = restClient.get()
                    .uri(TOGGL_API + "/me/time_entries/current")
                    .header(HttpHeaders.AUTHORIZATION, auth)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(Map.of("data", response != null ? response : Map.of()));
        } catch (Exception e) {
            return ActionResult.failure("Toggl getCurrentTimeEntry failed: " + e.getMessage());
        }
    }

    @ActionMapping(appKey = "toggl", actionKey = "startTimer")
    @SuppressWarnings("unchecked")
    public ActionResult startTimer(ActionContext context) {
        String auth = getAuth(context);
        if (auth == null) return ActionResult.failure("Toggl requires an 'apiToken' or 'apiKey'");

        Map<String, Object> config = context.configuration();
        String workspaceId = str(config, "workspaceId");
        String description = str(config, "description");

        if (workspaceId == null) return ActionResult.failure("'workspaceId' is required");

        try {
            Map<String, Object> body = new HashMap<>();
            if (description != null) body.put("description", description);
            body.put("workspace_id", Integer.parseInt(workspaceId));
            body.put("created_with", "crescendo");
            body.put("duration", -1);
            body.put("start", java.time.OffsetDateTime.now().toString());

            String projectId = str(config, "projectId");
            if (projectId != null) body.put("project_id", Integer.parseInt(projectId));

            Map<String, Object> response = restClient.post()
                    .uri(TOGGL_API + "/workspaces/" + workspaceId + "/time_entries")
                    .header(HttpHeaders.AUTHORIZATION, auth)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(Map.of("data", response));
        } catch (Exception e) {
            return ActionResult.failure("Toggl startTimer failed: " + e.getMessage());
        }
    }

    @ActionMapping(appKey = "toggl", actionKey = "stopTimer")
    @SuppressWarnings("unchecked")
    public ActionResult stopTimer(ActionContext context) {
        String auth = getAuth(context);
        if (auth == null) return ActionResult.failure("Toggl requires an 'apiToken' or 'apiKey'");

        Map<String, Object> config = context.configuration();
        String workspaceId = str(config, "workspaceId");
        String timeEntryId = str(config, "timeEntryId");

        if (workspaceId == null || timeEntryId == null) {
            return ActionResult.failure("'workspaceId' and 'timeEntryId' are required");
        }

        try {
            Map<String, Object> response = restClient.patch()
                    .uri(TOGGL_API + "/workspaces/" + workspaceId + "/time_entries/" + timeEntryId + "/stop")
                    .header(HttpHeaders.AUTHORIZATION, auth)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(Map.of("data", response));
        } catch (Exception e) {
            return ActionResult.failure("Toggl stopTimer failed: " + e.getMessage());
        }
    }
}
