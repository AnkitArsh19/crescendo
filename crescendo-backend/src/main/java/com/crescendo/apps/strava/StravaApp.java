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
        return new App("strava", "Strava", "Log activities, track workouts, and manage athletic data",
                "https://www.google.com/s2/favicons?domain=strava.com&sz=128", AuthType.OAUTH2,
                List.of(
                    Map.of("triggerKey", "activity-created", "name", "New Activity",
                        "description", "Triggers when a new activity is recorded",
                        "configSchema", List.of()),
                    Map.of("triggerKey", "activity-updated", "name", "Activity Updated",
                        "description", "Triggers when an activity is edited",
                        "configSchema", List.of())
                ),
                List.of(
                    Map.of("actionKey", "create-activity", "name", "Create Activity",
                        "description", "Log a new manual activity",
                        "configSchema", List.of(
                            Map.of("key", "name", "label", "Activity Name", "type", "text", "required", true,
                                   "placeholder", "Morning Run", "helpText", "Activity title"),
                            Map.of("key", "type", "label", "Type", "type", "select", "required", true,
                                   "options", List.of(
                                       Map.of("value", "Run", "label", "Run"),
                                       Map.of("value", "Ride", "label", "Ride"),
                                       Map.of("value", "Walk", "label", "Walk"),
                                       Map.of("value", "Swim", "label", "Swim"),
                                       Map.of("value", "Hike", "label", "Hike"),
                                       Map.of("value", "Workout", "label", "Workout")
                                   ), "helpText", "Activity type"),
                            Map.of("key", "startDate", "label", "Start Time", "type", "text", "required", true,
                                   "placeholder", "2025-03-15T08:00:00Z", "helpText", "ISO 8601 start time"),
                            Map.of("key", "duration", "label", "Duration (seconds)", "type", "text", "required", true,
                                   "placeholder", "3600", "helpText", "Elapsed time in seconds"),
                            Map.of("key", "distance", "label", "Distance (meters)", "type", "text", "required", false,
                                   "placeholder", "5000", "helpText", "Distance in meters"),
                            Map.of("key", "description", "label", "Description", "type", "textarea", "required", false,
                                   "helpText", "Activity notes"))),
                    Map.of("actionKey", "update-activity", "name", "Update Activity",
                        "description", "Edit an existing activity",
                        "configSchema", List.of(
                            Map.of("key", "activityId", "label", "Activity ID", "type", "text", "required", true,
                                   "helpText", "Strava activity ID"),
                            Map.of("key", "name", "label", "Name", "type", "text", "required", false, "helpText", "Updated name"),
                            Map.of("key", "description", "label", "Description", "type", "textarea", "required", false, "helpText", "Updated description"))),
                    Map.of("actionKey", "get-activities", "name", "Get Activities",
                        "description", "Retrieve recent activities",
                        "configSchema", List.of(
                            Map.of("key", "perPage", "label", "Max Results", "type", "text", "required", false,
                                   "placeholder", "10", "helpText", "Number of activities to return"))),
                    Map.of("actionKey", "get-athlete", "name", "Get Athlete Profile",
                        "description", "Retrieve the authenticated athlete's profile",
                        "configSchema", List.of())
                )
        ).credentialSchema(List.of()).category("fun").helpUrl("https://www.strava.com/settings/api");
    }
}
