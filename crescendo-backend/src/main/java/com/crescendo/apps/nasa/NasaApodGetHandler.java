package com.crescendo.apps.nasa;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@ActionMapping(appKey = "nasa-apod", actionKey = "get-apod")
public class NasaApodGetHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(NasaApodGetHandler.class);
    private static final String NASA_APOD_API = "https://api.nasa.gov/planetary/apod";

    @Value("${crescendo.platform.nasa-api-key:}")
    private String platformApiKey;

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        // Use platform-managed key first, fall back to user-provided credentials
        String apiKey = (platformApiKey != null && !platformApiKey.isBlank())
                ? platformApiKey
                : (creds != null ? (String) creds.get("apiKey") : null);
        if (apiKey == null || apiKey.isBlank()) {
            return ActionResult.failure("NASA APOD requires an API key (configure in application.properties or provide in connection)");
        }

        String date = config.get("date") != null ? config.get("date").toString() : null;

        StringBuilder uri = new StringBuilder(NASA_APOD_API + "?api_key=" + apiKey);
        if (date != null && !date.isBlank()) {
            uri.append("&date=").append(date);
        }

        try {
            String response = RestClient.create()
                    .get()
                    .uri(uri.toString())
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[nasa-apod] APOD fetched successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[nasa-apod] Fetch APOD failed", e);
            return ActionResult.failure("Failed to fetch NASA APOD: " + e.getMessage());
        }
    }
}
