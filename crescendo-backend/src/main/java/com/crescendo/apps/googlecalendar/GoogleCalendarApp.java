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

        var eventIdField = Map.of("key", "eventId", "label", "Event ID",
                "type", "text", "required", true,
                "helpText", "The ID of the event (from a trigger or search)");

        return new App("google-calendar", "Google Calendar", """
                Google Calendar is a time-management and scheduling calendar service. The Crescendo Google Calendar app enables you to automate event creation, updates, and schedule monitoring.

                **What you can do with Google Calendar in Crescendo:**
                - Trigger a workflow 10 minutes before a meeting starts to send an agenda
                - Create a new event automatically when a Calendly meeting is booked
                - Sync your Google Calendar out-of-office status to Slack
                - Log total hours spent in meetings per week to a Google Sheet

                **Triggers available:**
                - Event Started — trigger workflows when an event begins
                - Event Created/Updated — trigger workflows when schedule changes

                **Actions available:**
                - Create Event — schedule a new meeting
                - Update Event — change event details
                - Find Event — search your calendar

                **Who should use this:** Assistants, sales teams booking demos, and professionals optimizing their daily schedules.

                **Authentication:** OAuth 2.0 (connect your Google account).
                """,
                "https://ssl.gstatic.com/images/branding/product/2x/calendar_2020q4_48dp.png", AuthType.OAUTH2,

                // ═══ TRIGGERS ═══
                List.of(
                    Map.of(
                        "triggerKey", "event-created",
                        "name", "Event Created",
                        "description", "Triggers when a new calendar event is created",
                        "configSchema", List.of(
                            calendarField,
                            Map.of("key", "searchQuery", "label", "Search Text",
                                   "type", "text", "required", false,
                                   "placeholder", "Standup",
                                   "helpText", "Optionally filter by event title/description")
                        )
                    ),
                    Map.of(
                        "triggerKey", "event-updated",
                        "name", "Event Updated",
                        "description", "Triggers when a calendar event is updated",
                        "configSchema", List.of(calendarField)
                    ),
                    Map.of(
                        "triggerKey", "event-start",
                        "name", "Event Starting",
                        "description", "Triggers when a calendar event is about to start",
                        "configSchema", List.of(
                            calendarField,
                            Map.of("key", "minutesBefore", "label", "Minutes Before",
                                   "type", "text", "required", false,
                                   "placeholder", "15",
                                   "helpText", "Trigger this many minutes before the event starts (default: 0)")
                        )
                    ),
                    Map.of(
                        "triggerKey", "event-ended",
                        "name", "Event Ended",
                        "description", "Triggers when a calendar event has concluded",
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
                        "actionKey", "create",
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
                            Map.of("key", "location", "label", "Location",
                                   "type", "text", "required", false,
                                   "placeholder", "Conference Room A",
                                   "helpText", "Event location"),
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
                        "actionKey", "update",
                        "name", "Update Event",
                        "description", "Modify an existing calendar event",
                        "configSchema", List.of(
                            calendarField, eventIdField,
                            Map.of("key", "summary", "label", "Event Title",
                                   "type", "text", "required", false,
                                   "helpText", "New event title (leave blank to keep current)"),
                            Map.of("key", "start", "label", "Start Date/Time",
                                   "type", "text", "required", false,
                                   "helpText", "New start datetime in ISO 8601 format"),
                            Map.of("key", "end", "label", "End Date/Time",
                                   "type", "text", "required", false,
                                   "helpText", "New end datetime in ISO 8601 format"),
                            Map.of("key", "description", "label", "Description",
                                   "type", "textarea", "required", false,
                                   "helpText", "Updated event description"),
                            Map.of("key", "location", "label", "Location",
                                   "type", "text", "required", false,
                                   "helpText", "Updated event location")
                        )
                    ),
                    Map.of(
                        "actionKey", "get",
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
                            Map.of("key", "timeMax", "label", "Before Date",
                                   "type", "text", "required", false,
                                   "placeholder", "2025-04-01T00:00:00Z",
                                   "helpText", "Only show events before this date"),
                            Map.of("key", "maxResults", "label", "Max Results",
                                   "type", "text", "required", false,
                                   "placeholder", "10",
                                   "helpText", "Maximum events to return"),
                            Map.of("key", "singleEvents", "label", "Expand Recurring Events",
                                   "type", "select", "required", false,
                                   "options", List.of(Map.of("label", "True", "value", "true"), Map.of("label", "False", "value", "false")),
                                   "helpText", "Whether to expand recurring events into instances"),
                            Map.of("key", "iCalUID", "label", "iCalUID",
                                   "type", "text", "required", false,
                                   "helpText", "Specifies event ID in the iCalendar format"),
                            Map.of("key", "maxAttendees", "label", "Max Attendees",
                                   "type", "text", "required", false,
                                   "helpText", "The maximum number of attendees to include in the response"),
                            Map.of("key", "orderBy", "label", "Order By",
                                   "type", "select", "required", false,
                                   "options", List.of(Map.of("label", "Start Time", "value", "startTime"), Map.of("label", "Updated", "value", "updated")),
                                   "helpText", "Order by start date/time or last modification time"),
                            Map.of("key", "showDeleted", "label", "Show Deleted",
                                   "type", "select", "required", false,
                                   "options", List.of(Map.of("label", "True", "value", "true"), Map.of("label", "False", "value", "false")),
                                   "helpText", "Whether to include deleted events"),
                            Map.of("key", "timeZone", "label", "Timezone",
                                   "type", "text", "required", false,
                                   "helpText", "Time zone used in the response"),
                            Map.of("key", "updatedMin", "label", "Updated After",
                                   "type", "text", "required", false,
                                   "placeholder", "2025-03-01T00:00:00Z",
                                   "helpText", "Only show events updated after this date")
                        )
                    ),
                    Map.of(
                        "actionKey", "delete",
                        "name", "Delete Event",
                        "description", "Delete a calendar event by its ID",
                        "configSchema", List.of(calendarField, eventIdField)
                    ),
                    Map.of(
                        "actionKey", "addAttendee",
                        "name", "Add Attendees to Event",
                        "description", "Invite people to an existing calendar event",
                        "configSchema", List.of(
                            calendarField, eventIdField,
                            Map.of("key", "attendees", "label", "Attendee Emails",
                                   "type", "text", "required", true,
                                   "placeholder", "alice@example.com, bob@example.com",
                                   "helpText", "Comma-separated email addresses to invite")
                        )
                    ),
                    Map.of(
                        "actionKey", "getAll",
                        "name", "List Events",
                        "description", "List upcoming calendar events",
                        "configSchema", List.of(
                            calendarField,
                            Map.of("key", "maxResults", "label", "Max Results",
                                   "type", "text", "required", false,
                                   "placeholder", "10",
                                   "helpText", "Maximum number of events to return")
                        )
                    )
                )
        )
        .credentialSchema(List.of())
        .category("productivity")
        .helpUrl("https://console.cloud.google.com/");
    }
}