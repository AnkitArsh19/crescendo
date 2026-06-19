package com.crescendo.apps.weather;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@ActionMapping(appKey = "weather", actionKey = "get-onecall")
public class WeatherOneCallHandler implements ActionHandler {
    @Value("${crescendo.platform.weather-api-key:}")
    private String platformApiKey;

    @Override
public ActionResult execute(ActionContext c) {
        try {
            String key = apiKey(c);
            if (key.isBlank()) {
                return ActionResult.failure("Weather One Call requires an OpenWeather API key");
            }
            String res = RestClient.create().get()
                    .uri("https://api.openweathermap.org/data/3.0/onecall?lat={lat}&lon={lon}&units={units}&appid={key}",
                            c.configuration().get("lat"), c.configuration().get("lon"),
                            c.configuration().getOrDefault("units", "metric"), key)
                    .retrieve().body(String.class);
            return ActionResult.success(Map.of("response", res));
        } catch (Exception e) {
            if (e instanceof RestClientResponseException r) {
                return ActionResult.failure("Weather one call failed: " + r.getResponseBodyAsString());
            }
            return ActionResult.failure("Weather one call failed: " + e.getMessage());
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
