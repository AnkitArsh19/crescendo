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
        return new App("nasa-apod", "NASA APOD", "Get NASA's Astronomy Picture of the Day",
                "/icons/nasa.svg", AuthType.NONE,
                List.of(),
                List.of(Map.of(
                    "actionKey", "get-apod",
                    "name", "Get APOD",
                    "description", "Retrieve today's astronomy picture and explanation",
                    "configSchema", List.of(
                        Map.of("key", "date", "label", "Date (optional)",
                               "type", "text", "required", false,
                               "placeholder", "2026-01-15",
                               "helpText", "Specific date in YYYY-MM-DD format (default: today)")
                    )
                ))
        )
        .credentialSchema(List.of())
        .category("fun")
        .helpUrl("https://api.nasa.gov/");
    }
}
