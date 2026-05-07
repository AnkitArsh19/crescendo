package com.crescendo.apps.googlecalendar;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GoogleCalendarApp implements AppDefinition {

    @Override
    public App toApp() {
        var calendarField = Map.of("key", "calendarId", "label", "Calendar",
                "type", "dynamic_dropdown", "resourceType", "calendars",
                "required", true,
                "helpText", "Select which calendar to use");

        return new App("google-calendar", "Google Calendar", "Create, update, and watch calendar events",
                "/icons/google-calendar.svg", AuthType.OAUTH2,

                // ═══ TRIGGERS ═══
                List.of(
                    Map.of(
                        "triggerKey", "event-created",
                        "name", "Event Created",
                        "description", "Triggers when a new calendar event is created",
                        "configSchema", List.of(calendarField)
                    ),
                    Map.of(
                        "triggerKey", "event-updated",
                        "name", "Event Updated",
                        "description", "Triggers when a calendar event is updated",
                        "configSchema", List.of(calendarField)
                    ),
                    Map.of(
                        "triggerKey", "event-cancelled",
                        "name", "Event Cancelled",
                        "description", "Triggers when a calendar event is cancelled or deleted",
                        "configSchema", List.of(calendarField)
                    )
                ),

                // ═══ ACTIONS ═══
                List.of(
                    Map.of(
                        "actionKey", "create-event",
                        "name", "Create Event",
                        "description", "Create a new calendar event",
                        "configSchema", List.of(
                            calendarField,
                            Map.of("key", "summary", "label", "Event Title",
                                   "type", "text", "required", true,
                                   "placeholder", "Team Standup",
                                   "helpText", "Title of the calendar event"),
                            Map.of("key", "start", "label", "Start Date/Time",
                                   "type", "text", "required", true,
                                   "placeholder", "2025-03-15T10:00:00",
                                   "helpText", "Start datetime in ISO 8601 format"),
                            Map.of("key", "end", "label", "End Date/Time",
                                   "type", "text", "required", true,
                                   "placeholder", "2025-03-15T11:00:00",
                                   "helpText", "End datetime in ISO 8601 format"),
                            Map.of("key", "description", "label", "Description",
                                   "type", "textarea", "required", false,
                                   "helpText", "Optional event description"),
                            Map.of("key", "attendees", "label", "Attendees",
                                   "type", "text", "required", false,
                                   "placeholder", "alice@example.com, bob@example.com",
                                   "helpText", "Comma-separated attendee emails"),
                            Map.of("key", "timeZone", "label", "Timezone",
                                   "type", "text", "required", false,
                                   "placeholder", "Asia/Kolkata",
                                   "helpText", "IANA timezone (default: UTC)")
                        )
                    ),
                    Map.of(
                        "actionKey", "find-event",
                        "name", "Find Event",
                        "description", "Search for an event by title or date range",
                        "configSchema", List.of(
                            calendarField,
                            Map.of("key", "query", "label", "Search Text",
                                   "type", "text", "required", false,
                                   "placeholder", "Standup",
                                   "helpText", "Search for events by title/description"),
                            Map.of("key", "timeMin", "label", "After Date",
                                   "type", "text", "required", false,
                                   "placeholder", "2025-03-01T00:00:00Z",
                                   "helpText", "Only show events after this date"),
                            Map.of("key", "maxResults", "label", "Max Results",
                                   "type", "text", "required", false,
                                   "placeholder", "10",
                                   "helpText", "Maximum events to return")
                        )
                    ),
                    Map.of(
                        "actionKey", "list-events",
                        "name", "List Events",
                        "description", "List upcoming calendar events",
                        "configSchema", List.of(
                            calendarField,
                            Map.of("key", "maxResults", "label", "Max Results",
                                   "type", "text", "required", false,
                                   "placeholder", "10",
                                   "helpText", "Maximum number of events to return")
                        )
                    ),
                    Map.of(
                        "actionKey", "delete-event",
                        "name", "Delete Event",
                        "description", "Delete a calendar event by its ID",
                        "configSchema", List.of(
                            calendarField,
                            Map.of("key", "eventId", "label", "Event ID",
                                   "type", "text", "required", true,
                                   "helpText", "The ID of the event to delete (from a trigger or find)")
                        )
                    )
                )
        )
        .credentialSchema(List.of())
        .category("productivity")
        .helpUrl("https://console.cloud.google.com/");
    }
}