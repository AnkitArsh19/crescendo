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

@ActionMapping(appKey = "strava", actionKey = "get-athlete")
public class StravaGetAthleteHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(StravaGetAthleteHandler.class);
    private static final String STRAVA_API = "https://www.strava.com/api/v3";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> creds = context.credentials();

        String token = creds != null ? (String) creds.get("accessToken") : null;
        if (token == null || token.isBlank()) return ActionResult.failure("Strava requires 'accessToken'");

        try {
            String response = RestClient.create()
                    .get()
                    .uri(STRAVA_API + "/athlete")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[strava] Athlete profile fetched successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[strava] Get athlete failed", e);
            return ActionResult.failure("Strava get athlete failed: " + e.getMessage());
        }
    }
}
