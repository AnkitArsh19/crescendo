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
        return new App("nasa", "NASA", """
                The National Aeronautics and Space Administration (NASA) provides open APIs to access space data and imagery. The Crescendo NASA app gives you access to the Astronomy Picture of the Day and Mars Rover photos.

                **What you can do with NASA in Crescendo:**
                - Post the daily NASA picture to a Discord channel every morning
                - Collect and archive Mars Rover photos in a Google Drive folder
                - Send space facts and images to educational Telegram groups
                - Generate descriptive alt-text for space images using AI

                **Actions available:**
                - Get APOD — fetch the Astronomy Picture of the Day
                - Get Mars Photos — retrieve images taken by various Mars rovers

                **Who should use this:** Space enthusiasts, educators, community managers, and anyone looking to add a touch of space to their daily routine.

                **Authentication:** None required (uses a public DEMO_KEY by default).
                """,
                "https://www.google.com/s2/favicons?domain=nasa.gov&sz=128", AuthType.NONE,
                List.of(),
                List.of(
                    Map.of("actionKey", "nasa:apod:get", "name", "Get Astronomy Picture",
                        "description", "Get NASA's Astronomy Picture of the Day",
                        "configSchema", List.of(
                            Map.of("key", "date", "label", "Date", "type", "text", "required", false,
                                   "placeholder", "2026-01-15", "helpText", "YYYY-MM-DD (default: today)"))),
                    Map.of("actionKey", "nasa:mars:getPhotos", "name", "Get Mars Rover Photos",
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
