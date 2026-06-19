package com.crescendo.apps.icalendar;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@ActionMapping(appKey = "icalendar", actionKey = "create-event")
public class ICalendarCreateEventHandler implements ActionHandler {

    private static final DateTimeFormatter ICS_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    @Override
public ActionResult execute(ActionContext context) {
        try {
            String title = value(context, "title", "");
            String start = value(context, "start", "");
            String end = value(context, "end", "");
            if (title.isBlank() || start.isBlank() || end.isBlank()) {
                return ActionResult.failure("iCalendar title, start, and end are required");
            }
            String uid = UUID.randomUUID() + "@crescendo.run";
            String ics = "BEGIN:VCALENDAR\r\n"
                    + "VERSION:2.0\r\n"
                    + "PRODID:-//Crescendo//Automation//EN\r\n"
                    + "BEGIN:VEVENT\r\n"
                    + "UID:" + uid + "\r\n"
                    + "DTSTAMP:" + format(OffsetDateTime.now()) + "\r\n"
                    + "DTSTART:" + format(OffsetDateTime.parse(start)) + "\r\n"
                    + "DTEND:" + format(OffsetDateTime.parse(end)) + "\r\n"
                    + "SUMMARY:" + escape(title) + "\r\n"
                    + optional("DESCRIPTION", value(context, "description", ""))
                    + optional("LOCATION", value(context, "location", ""))
                    + "END:VEVENT\r\n"
                    + "END:VCALENDAR\r\n";
            return ActionResult.success(Map.of("uid", uid, "ics", ics, "fileName", safeFileName(title) + ".ics"));
        } catch (Exception e) {
            return ActionResult.failure("Create ICS event failed: " + e.getMessage());
        }
    }

    private String value(ActionContext context, String key, String fallback) {
        Object value = context.configuration().get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private String format(OffsetDateTime dateTime) {
        return dateTime.withOffsetSameInstant(ZoneOffset.UTC).format(ICS_TIME);
    }

    private String optional(String key, String value) {
        return value == null || value.isBlank() ? "" : key + ":" + escape(value) + "\r\n";
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace(",", "\\,")
                .replace(";", "\\;")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private String safeFileName(String title) {
        String cleaned = title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return cleaned.isBlank() ? "event" : cleaned;
    }
}
