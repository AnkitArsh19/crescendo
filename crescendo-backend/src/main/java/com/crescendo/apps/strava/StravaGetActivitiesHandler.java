package com.crescendo.apps.strava;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@ActionMapping(appKey = "strava", actionKey = "get-activities")
public class StravaGetActivitiesHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(StravaGetActivitiesHandler.class);
    private static final String STRAVA_API = "https://www.strava.com/api/v3";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String token = creds != null ? (String) creds.get("accessToken") : null;
        if (token == null || token.isBlank()) return ActionResult.failure("Strava requires 'accessToken'");

        int perPage = 30, page = 1;
        if (config.containsKey("perPage")) {
            try { perPage = Integer.parseInt(config.get("perPage").toString()); }
            catch (NumberFormatException ignored) {}
        }
        if (config.containsKey("page")) {
            try { page = Integer.parseInt(config.get("page").toString()); }
            catch (NumberFormatException ignored) {}
        }

        try {
            String response = RestClient.create()
                    .get()
                    .uri(STRAVA_API + "/athlete/activities?per_page=" + perPage + "&page=" + page)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[strava] Activities fetched successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[strava] Get activities failed", e);
            return ActionResult.failure("Strava get activities failed: " + e.getMessage());
        }
    }
}
