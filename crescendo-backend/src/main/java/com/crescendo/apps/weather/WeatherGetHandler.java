package com.crescendo.apps.weather;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.Map;

@ActionMapping(appKey = "weather", actionKey = "get-weather")
public class WeatherGetHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(WeatherGetHandler.class);
    private static final String OWM_API = "https://api.openweathermap.org/data/2.5/weather";

    @Value("${crescendo.platform.weather-api-key:}")
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
            return ActionResult.failure(
                    "Weather requires an API key (configure in application.properties or provide in connection)");
        }

        String city = config.get("city") != null ? config.get("city").toString() : null;
        if (city == null || city.isBlank()) {
            // Also check "location" for backward compat
            city = config.get("location") != null ? config.get("location").toString() : null;
        }
        if (city == null || city.isBlank())
            return ActionResult.failure("'city' is required");

        String units = config.getOrDefault("units", "metric").toString();

        try {
            String response = RestClient.create()
                    .get()
                    .uri(OWM_API + "?q={city}&units={units}&appid={apiKey}", city, units, apiKey)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            output.put("city", city);
            output.put("units", units);
            logger.info("[weather] Weather fetched for city={}", city);
            return ActionResult.success(output);
        } catch (Exception e) {
            if (e instanceof RestClientResponseException r) {
                return ActionResult.failure("Weather fetch failed: " + r.getResponseBodyAsString());
            }
            logger.error("[weather] Get weather failed for city={}", city, e);
            return ActionResult.failure("Weather fetch failed: " + e.getMessage());
        }
    }
}
