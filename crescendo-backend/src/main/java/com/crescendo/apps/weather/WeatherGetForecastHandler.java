package com.crescendo.apps.weather;

import com.crescendo.execution.action.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import java.util.*;

/**
 * Gets 5-day forecast via OpenWeatherMap /data/2.5/forecast.
 */
@ActionMapping(appKey = "weather", actionKey = "get-forecast")
public class WeatherGetForecastHandler implements ActionHandler {
    private static final Logger logger = LoggerFactory.getLogger(WeatherGetForecastHandler.class);
    private final RestClient restClient = RestClient.create();

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String city = config.get("city") != null ? config.get("city").toString() : null;
        if (city == null) return ActionResult.failure("'city' is required");
        String units = config.getOrDefault("units", "metric").toString();
        String days = config.getOrDefault("days", "5").toString();

        try {
            String url = "https://api.openweathermap.org/data/2.5/forecast?q="
                    + java.net.URLEncoder.encode(city, "UTF-8")
                    + "&units=" + units + "&cnt=" + (Integer.parseInt(days) * 8)
                    + "&appid=demo";

            Map<String, Object> resp = restClient.get().uri(url).retrieve().body(Map.class);

            Map<String, Object> out = new HashMap<>();
            out.put("provider", "weather");
            out.put("action", "get-forecast");
            out.put("city", city);
            out.put("forecastCount", resp != null && resp.containsKey("list") ? ((List<?>)resp.get("list")).size() : 0);
            out.put("forecast", resp != null ? resp.get("list") : null);
            return ActionResult.success(out);
        } catch (Exception e) {
            return ActionResult.failure("Weather forecast failed: " + e.getMessage());
        }
    }
}
