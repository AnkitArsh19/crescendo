package com.crescendo.apps.weather;

import com.crescendo.execution.action.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import java.util.*;

/**
 * Gets 5-day forecast via OpenWeatherMap /data/2.5/forecast.
 */
@ActionMapping(appKey = "weather", actionKey = "get-forecast")
@SuppressWarnings("unchecked")
public class WeatherGetForecastHandler implements ActionHandler {
    private final RestClient restClient = RestClient.create();

    @Value("${crescendo.platform.weather-api-key:}")
    private String platformApiKey;

    @Override
public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String city = config.get("city") != null ? config.get("city").toString() : null;
        if (city == null) return ActionResult.failure("'city' is required");
        String units = config.getOrDefault("units", "metric").toString();
        String days = config.getOrDefault("days", "5").toString();
        String apiKey = apiKey(context);
        if (apiKey.isBlank()) {
            return ActionResult.failure("Weather forecast requires an OpenWeather API key");
        }

        try {
            String url = "https://api.openweathermap.org/data/2.5/forecast?q="
                    + java.net.URLEncoder.encode(city, "UTF-8")
                    + "&units=" + units + "&cnt=" + (Integer.parseInt(days) * 8)
                    + "&appid=" + apiKey;

            Map<String, Object> resp = restClient.get().uri(url).retrieve().body(Map.class);

            Map<String, Object> out = new HashMap<>();
            out.put("provider", "weather");
            out.put("action", "get-forecast");
            out.put("city", city);
            out.put("forecastCount", resp != null && resp.containsKey("list") ? ((List<?>)resp.get("list")).size() : 0);
            out.put("forecast", resp != null ? resp.get("list") : null);
            return ActionResult.success(out);
        } catch (Exception e) {
            if (e instanceof RestClientResponseException r) {
                return ActionResult.failure("Weather forecast failed: " + r.getResponseBodyAsString());
            }
            return ActionResult.failure("Weather forecast failed: " + e.getMessage());
        }
    }

    private String apiKey(ActionContext context) {
        Map<String, Object> creds = context.credentials();
        if (platformApiKey != null && !platformApiKey.isBlank()) {
            return platformApiKey;
        }
        Object key = creds != null ? creds.get("apiKey") : null;
        return key == null ? "" : String.valueOf(key);
    }
}
