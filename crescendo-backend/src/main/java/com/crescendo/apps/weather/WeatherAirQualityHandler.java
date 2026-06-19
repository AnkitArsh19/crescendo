package com.crescendo.apps.weather;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@ActionMapping(appKey = "weather", actionKey = "get-air-quality")
public class WeatherAirQualityHandler implements ActionHandler {
    @Value("${crescendo.platform.weather-api-key:}")
    private String platformApiKey;

    @Override
public ActionResult execute(ActionContext c) {
        try {
            String key = apiKey(c);
            if (key.isBlank()) {
                return ActionResult.failure("Weather air quality requires an OpenWeather API key");
            }
            String res = RestClient.create().get()
                    .uri("https://api.openweathermap.org/data/2.5/air_pollution?lat={lat}&lon={lon}&appid={key}",
                            c.configuration().get("lat"), c.configuration().get("lon"), key)
                    .retrieve().body(String.class);
            return ActionResult.success(Map.of("response", res));
        } catch (Exception e) {
            if (e instanceof RestClientResponseException r) {
                return ActionResult.failure("Weather air quality failed: " + r.getResponseBodyAsString());
            }
            return ActionResult.failure("Weather air quality failed: " + e.getMessage());
        }
    }

    private String apiKey(ActionContext c) {
        if (platformApiKey != null && !platformApiKey.isBlank()) {
            return platformApiKey;
        }
        Object value = c.credentials() != null ? c.credentials().get("apiKey") : null;
        return value == null ? "" : String.valueOf(value);
    }
}
