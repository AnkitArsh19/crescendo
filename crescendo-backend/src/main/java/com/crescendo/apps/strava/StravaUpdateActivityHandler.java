package com.crescendo.apps.strava;

import com.crescendo.execution.action.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import java.util.*;

/**
 * Updates an existing Strava activity via PUT /api/v3/activities/{id}.
 */
@ActionMapping(appKey = "strava", actionKey = "update-activity")
@SuppressWarnings("unchecked")
public class StravaUpdateActivityHandler implements ActionHandler {
    private final RestClient restClient = RestClient.create();

    @Override
public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();
        String token = creds != null ? (String) creds.get("accessToken") : null;
        if (token == null) return ActionResult.failure("Strava requires an OAuth2 accessToken");

        String activityId = config.get("activityId") != null ? config.get("activityId").toString() : null;
        if (activityId == null) return ActionResult.failure("'activityId' is required");

        try {
            Map<String, Object> body = new HashMap<>();
            if (config.containsKey("name")) body.put("name", config.get("name"));
            if (config.containsKey("description")) body.put("description", config.get("description"));
            if (config.containsKey("type")) body.put("type", config.get("type"));

            Map<String, Object> resp = restClient.put()
                    .uri("https://www.strava.com/api/v3/activities/" + activityId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body).retrieve().body(Map.class);

            Map<String, Object> out = new HashMap<>();
            out.put("provider", "strava");
            out.put("action", "update-activity");
            out.put("activityId", activityId);
            out.put("name", resp != null ? resp.get("name") : null);
            return ActionResult.success(out);
        } catch (Exception e) {
            return ActionResult.failure("Strava update-activity failed: " + e.getMessage());
        }
    }
}
