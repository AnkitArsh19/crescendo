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
        return new App("weather", "Weather", """
                The Weather app provides access to current conditions, forecasts, and historical weather data. The Crescendo Weather app allows you to automate workflows based on meteorological conditions in any city worldwide.

                **What you can do with Weather in Crescendo:**
                - Send daily forecast summaries to a Telegram or Slack group
                - Trigger notifications when the temperature drops below a certain threshold
                - Log air quality data to Google Sheets for analysis
                - Automate reminders to bring an umbrella based on rain probability

                **Actions available:**
                - Get Current Weather — fetch the current temperature and conditions for a city
                - Get 5-Day Forecast — retrieve upcoming weather predictions

                **Who should use this:** Outdoor event planners, individuals wanting personalized daily briefings, and teams tracking environmental conditions.

                **Authentication:** None natively (uses a demo API key by default, but you can provide your own OpenWeatherMap key).
                """,
                "https://www.google.com/s2/favicons?domain=openweathermap.org&sz=128", AuthType.NONE,
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
                                   "placeholder", "5", "helpText", "Number of days (1-5)"))),
                    Map.of("actionKey", "get-air-quality", "name", "Get Air Quality",
                        "description", "Get air quality by latitude and longitude",
                        "configSchema", List.of(
                            Map.of("key", "lat", "label", "Latitude", "type", "text", "required", true),
                            Map.of("key", "lon", "label", "Longitude", "type", "text", "required", true))),
                    Map.of("actionKey", "get-onecall", "name", "Get Hourly / Daily Forecast",
                        "description", "Get OpenWeather One Call data; this endpoint may require One Call access on the user's OpenWeather account",
                        "configSchema", List.of(
                            Map.of("key", "lat", "label", "Latitude", "type", "text", "required", true),
                            Map.of("key", "lon", "label", "Longitude", "type", "text", "required", true),
                            Map.of("key", "units", "label", "Units", "type", "text", "required", false, "placeholder", "metric")))
                )
        ).credentialSchema(List.of()).category("productivity")
                .helpUrl("https://openweathermap.org/appid");
    }
}
