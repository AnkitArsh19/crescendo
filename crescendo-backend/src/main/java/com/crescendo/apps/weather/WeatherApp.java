package com.crescendo.apps.weather;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class WeatherApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("weather", "Weather", "Get current weather and forecasts for any location",
                "/icons/weather.svg", AuthType.NONE,
                List.of(),
                List.of(
                    Map.of("actionKey", "get-weather", "name", "Get Current Weather",
                        "description", "Fetch current weather for a location",
                        "configSchema", List.of(
                            Map.of("key", "city", "label", "City", "type", "text", "required", true,
                                   "placeholder", "Bangalore", "helpText", "City name"),
                            Map.of("key", "units", "label", "Units", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "metric", "label", "Celsius (°C)"),
                                       Map.of("value", "imperial", "label", "Fahrenheit (°F)"),
                                       Map.of("value", "standard", "label", "Kelvin (K)")
                                   ), "helpText", "Temperature units"))),
                    Map.of("actionKey", "get-forecast", "name", "Get Forecast",
                        "description", "Get 5-day weather forecast",
                        "configSchema", List.of(
                            Map.of("key", "city", "label", "City", "type", "text", "required", true,
                                   "placeholder", "London", "helpText", "City name"),
                            Map.of("key", "units", "label", "Units", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "metric", "label", "Celsius"),
                                       Map.of("value", "imperial", "label", "Fahrenheit")
                                   ), "helpText", "Temperature units"),
                            Map.of("key", "days", "label", "Days", "type", "text", "required", false,
                                   "placeholder", "5", "helpText", "Number of days (1-5)")))
                )
        ).credentialSchema(List.of()).category("productivity").helpUrl("https://openweathermap.org/appid");
    }
}
