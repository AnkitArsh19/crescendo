package com.crescendo.apps.nasa;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class NasaApodApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("nasa-apod", "NASA", "Astronomy Picture of the Day, Mars Rover photos, and more",
                "/icons/nasa.svg", AuthType.NONE,
                List.of(),
                List.of(
                    Map.of("actionKey", "get-apod", "name", "Get Astronomy Picture",
                        "description", "Get NASA's Astronomy Picture of the Day",
                        "configSchema", List.of(
                            Map.of("key", "date", "label", "Date", "type", "text", "required", false,
                                   "placeholder", "2026-01-15", "helpText", "YYYY-MM-DD (default: today)"))),
                    Map.of("actionKey", "get-mars-photos", "name", "Get Mars Rover Photos",
                        "description", "Get photos from Mars rovers",
                        "configSchema", List.of(
                            Map.of("key", "rover", "label", "Rover", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "curiosity", "label", "Curiosity"),
                                       Map.of("value", "perseverance", "label", "Perseverance"),
                                       Map.of("value", "opportunity", "label", "Opportunity")
                                   ), "helpText", "Mars rover"),
                            Map.of("key", "sol", "label", "Sol (Mars Day)", "type", "text", "required", false,
                                   "placeholder", "1000", "helpText", "Martian sol number")))
                )
        ).credentialSchema(List.of()).category("fun").helpUrl("https://api.nasa.gov/");
    }
}
