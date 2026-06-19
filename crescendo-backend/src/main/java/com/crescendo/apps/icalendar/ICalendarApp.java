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
        return new App("icalendar", "iCalendar", "Generate .ics calendar invite content",
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
