package com.crescendo.apps.icalendar;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ICalendarApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("icalendar", "iCalendar", """
                The iCalendar app is a built-in utility that allows you to programmatically generate standard `.ics` calendar invitation files that can be imported by Google Calendar, Outlook, and Apple Calendar.

                **What you can do with iCalendar in Crescendo:**
                - Generate an `.ics` file for a webinar registration and attach it to an automated welcome email via Brevo
                - Create custom calendar blocks for internal team events based on a Slack slash command
                - Automatically construct a calendar invite when a client fills out a Typeform booking request
                - Send dynamic "Save the Date" calendar attachments to event attendees

                **Actions available:**
                - Generate Invite — provide an event summary, start time, end time, and description to output a valid `text/calendar` string

                **Who should use this:** Event organizers, marketing teams, and HR professionals automating meeting schedules.

                **Authentication:** None required.
                """,
                "/icons/icalendar.svg", AuthType.NONE,
                List.of(),
                List.of(
                        Map.of("actionKey", "create-event", "name", "Create ICS Event",
                                "description", "Create iCalendar content for an event",
                                "configSchema", List.of(
                                        Map.of("key", "title", "label", "Title", "type", "text", "required", true),
                                        Map.of("key", "start", "label", "Start", "type", "text", "required", true,
                                                "placeholder", "2026-06-19T10:00:00+05:30"),
                                        Map.of("key", "end", "label", "End", "type", "text", "required", true,
                                                "placeholder", "2026-06-19T10:30:00+05:30"),
                                        Map.of("key", "description", "label", "Description", "type", "textarea", "required", false),
                                        Map.of("key", "location", "label", "Location", "type", "text", "required", false)))
                )
        ).credentialSchema(List.of()).category("core").helpUrl("https://icalendar.org/");
    }
}
