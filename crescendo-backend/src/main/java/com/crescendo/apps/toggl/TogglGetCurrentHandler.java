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

@ActionMapping(appKey = "toggl", actionKey = "get-current")
public class TogglGetCurrentHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(TogglGetCurrentHandler.class);
    private static final String TOGGL_API = "https://api.track.toggl.com/api/v9";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> creds = context.credentials();

        String apiToken = creds != null ? (String) creds.get("apiKey") : null;
        if (apiToken == null || apiToken.isBlank()) return ActionResult.failure("Toggl requires 'apiKey'");

        try {
            String auth = Base64.getEncoder().encodeToString((apiToken + ":api_token").getBytes());

            String response = RestClient.create()
                    .get()
                    .uri(TOGGL_API + "/me/time_entries/current")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + auth)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[toggl] Current timer fetched successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[toggl] Get current timer failed", e);
            return ActionResult.failure("Toggl get current timer failed: " + e.getMessage());
        }
    }
}
