package com.crescendo.apps.strava;

import com.crescendo.execution.action.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import java.util.*;

/**
 * Creates a manual activity in Strava via POST /api/v3/activities.
 */
@ActionMapping(appKey = "strava", actionKey = "create-activity")
@SuppressWarnings("unchecked")
public class StravaCreateActivityHandler implements ActionHandler {
    private final RestClient restClient = RestClient.create();

    @Override
public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();
        String token = creds != null ? (String) creds.get("accessToken") : null;
        if (token == null) return ActionResult.failure("Strava requires an OAuth2 accessToken");

        String name = config.get("name") != null ? config.get("name").toString() : null;
        String type = config.get("type") != null ? config.get("type").toString() : null;
        String startDate = config.get("startDate") != null ? config.get("startDate").toString() : null;
        String duration = config.get("duration") != null ? config.get("duration").toString() : null;
        if (name == null) return ActionResult.failure("'name' is required");
        if (type == null) return ActionResult.failure("'type' is required");
        if (startDate == null) return ActionResult.failure("'startDate' is required");
        if (duration == null) return ActionResult.failure("'duration' is required");

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("name", name);
            body.put("type", type);
            body.put("start_date_local", startDate);
            body.put("elapsed_time", Integer.parseInt(duration));
            if (config.containsKey("distance")) body.put("distance", Float.parseFloat(config.get("distance").toString()));
            if (config.containsKey("description")) body.put("description", config.get("description"));

            Map<String, Object> resp = restClient.post()
                    .uri("https://www.strava.com/api/v3/activities")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body).retrieve().body(Map.class);

            Map<String, Object> out = new HashMap<>();
            out.put("provider", "strava");
            out.put("action", "create-activity");
            out.put("activityId", resp != null ? resp.get("id") : null);
            out.put("name", name);
            return ActionResult.success(out);
        } catch (Exception e) {
            return ActionResult.failure("Strava create-activity failed: " + e.getMessage());
        }
    }
}
