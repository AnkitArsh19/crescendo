package com.crescendo.apps.strava;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class StravaApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("strava", "Strava", "Track activities and manage athletic data",
                "/icons/strava.svg", AuthType.OAUTH2,
                List.of(Map.of(
                    "triggerKey", "activity-created",
                    "name", "Activity Created",
                    "description", "Triggers when a new activity is recorded"
                )),
                List.of(Map.of(
                    "actionKey", "get-activities",
                    "name", "Get Activities",
                    "description", "Retrieve recent activities",
                    "configSchema", Map.of(
                        "perPage", "integer — number of activities to return (default: 10)"
                    )
                ))
        )
        .credentialSchema(List.of())
        .category("fun")
        .helpUrl("https://www.strava.com/settings/api");
    }
}
