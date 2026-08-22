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
        return new App("strava", "Strava", """
                Strava is a social fitness network that tracks cycling and running exercises. The Crescendo Strava app lets you log activities, track workouts, and manage your athletic data automatically.

                **What you can do with Strava in Crescendo:**
                - Log completed Pomodoro sessions as Strava activities
                - Post new activity summaries to a Slack or Discord channel
                - Export weekly training stats to Google Sheets
                - Notify your friends when you hit a new personal best

                **Actions available:**
                - Create Activity — manually log a workout
                - Get Athlete Stats — retrieve your performance data
                - Get Activities — fetch a list of your recent workouts

                **Who should use this:** Athletes tracking their training data, fitness communities sharing progress, and anyone automating their health metrics.

                **Authentication:** Bring Your Own Key (BYOK) / Custom OAuth 2.0. Strava requires developer accounts to have an active paid Strava subscription. Provide your personal Strava Developer Client ID & Secret from your Strava API Settings.
                """,
                "/icons/strava.svg", AuthType.OAUTH2,
                List.of(
                    Map.of("triggerKey", "activity-created", "name", "New Activity",
                        "description", "Triggers when a new activity is recorded",
                        "configSchema", List.of()),
                    Map.of("triggerKey", "activity-updated", "name", "Activity Updated",
                        "description", "Triggers when an activity is edited",
                        "configSchema", List.of())
                ),
                List.of(
                    Map.of("actionKey", "strava:activity:create", "name", "Create Activity",
                        "description", "Log a new manual activity",
                        "configSchema", List.of(
                            Map.of("key", "name", "label", "Activity Name", "type", "text", "required", true,
                                   "placeholder", "Morning Workout", "helpText", "Activity title"),
                            Map.of("key", "type", "label", "Activity Type", "type", "select", "required", true,
                                   "options", List.of(
                                       Map.of("value", "Run", "label", "Run"),
                                       Map.of("value", "Ride", "label", "Ride / Cycling"),
                                       Map.of("value", "Walk", "label", "Walk"),
                                       Map.of("value", "Swim", "label", "Swim"),
                                       Map.of("value", "Hike", "label", "Hike"),
                                       Map.of("value", "Workout", "label", "General Workout"),
                                       Map.of("value", "WeightTraining", "label", "Weight Training"),
                                       Map.of("value", "Yoga", "label", "Yoga"),
                                       Map.of("value", "VirtualRide", "label", "Virtual Ride (Zwift)"),
                                       Map.of("value", "VirtualRun", "label", "Virtual Run"),
                                       Map.of("value", "Crossfit", "label", "Crossfit"),
                                       Map.of("value", "Rowing", "label", "Rowing"),
                                       Map.of("value", "Elliptical", "label", "Elliptical"),
                                       Map.of("value", "StairStepper", "label", "Stair Stepper"),
                                       Map.of("value", "Golf", "label", "Golf")
                                   ), "helpText", "Select workout type"),
                            Map.of("key", "startDate", "label", "Start Time", "type", "datetime", "required", true,
                                   "default", "{{now}}", "placeholder", "{{now}} or pick a date/time", "helpText", "When activity started (defaults to workflow execution time)"),
                            Map.of("key", "duration", "label", "Duration (seconds)", "type", "select", "required", true,
                                   "options", List.of(
                                       Map.of("value", "900", "label", "15 minutes (900s)"),
                                       Map.of("value", "1800", "label", "30 minutes (1,800s)"),
                                       Map.of("value", "2700", "label", "45 minutes (2,700s)"),
                                       Map.of("value", "3600", "label", "1 hour (3,600s)"),
                                       Map.of("value", "5400", "label", "1.5 hours (5,400s)"),
                                       Map.of("value", "7200", "label", "2 hours (7,200s)")
                                   ), "default", "1800", "helpText", "Duration in seconds"),
                            Map.of("key", "distance", "label", "Distance (meters)", "type", "text", "required", false,
                                   "placeholder", "5000", "helpText", "Distance in meters (optional)"),
                            Map.of("key", "description", "label", "Description", "type", "textarea", "required", false,
                                   "helpText", "Activity notes or notes from earlier step"))),
                    Map.of("actionKey", "strava:activity:update", "name", "Update Activity",
                        "description", "Edit an existing activity",
                        "configSchema", List.of(
                            Map.of("key", "activityId", "label", "Activity ID", "type", "text", "required", true,
                                   "helpText", "Strava activity ID"),
                            Map.of("key", "name", "label", "Name", "type", "text", "required", false, "helpText", "Updated name"),
                            Map.of("key", "description", "label", "Description", "type", "textarea", "required", false, "helpText", "Updated description"))),
                    Map.of("actionKey", "strava:activity:getMany", "name", "Get Activities",
                        "description", "Retrieve recent activities",
                        "configSchema", List.of(
                            Map.of("key", "perPage", "label", "Max Results", "type", "text", "required", false,
                                   "placeholder", "10", "helpText", "Number of activities to return"))),
                    Map.of("actionKey", "strava:athlete:get", "name", "Get Athlete Profile",
                        "description", "Retrieve the authenticated athlete's profile",
                        "configSchema", List.of())
                )
        )
        .altAuthType(AuthType.APIKEY)
        .credentialSchema(List.of(
            Map.of("key", "clientId", "label", "Client ID", "type", "text", "required", true,
                   "placeholder", "e.g. 140396", "helpText", "Your Strava API Application Client ID", "authType", "APIKEY"),
            Map.of("key", "clientSecret", "label", "Client Secret", "type", "password", "required", true,
                   "placeholder", "e.g. 03e44618...", "helpText", "Your Strava API Application Client Secret", "authType", "APIKEY")
        ))
        .category("fun")
        .helpUrl("https://www.strava.com/settings/api");
    }
}
