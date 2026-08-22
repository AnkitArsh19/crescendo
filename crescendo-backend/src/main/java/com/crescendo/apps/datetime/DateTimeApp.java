package com.crescendo.apps.datetime;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for DateTime.
 */
@Component
public class DateTimeApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "dateTime",
                "Date & Time",
                """
                Manipulate date and time values.
                
                This integration provides operations for:
                - **Get Current Date**: Get the current date and time
                - **Add to a Date**: Add a specific amount of time to a date
                - **Subtract From a Date**: Subtract a specific amount of time from a date
                - **Format a Date**: Format a date into a specific format string
                - **Round a Date**: Round a date up or down
                - **Get Time Between Dates**: Calculate the difference between two dates
                - **Extract Part of a Date**: Extract a specific part (e.g., year, month) from a date
                """,
                "/icons/datetime.svg", // Generic icon
                AuthType.NONE,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "dateTime:getCurrentDate",
                                "name", "Get Current Date",
                                "description", "Get the current date and time",
                                "configSchema", List.of(
                                        Map.of("key", "includeTime", "label", "Include Time", "type", "boolean", "default", true),
                                        Map.of("key", "outputFieldName", "label", "Output Field Name", "type", "text", "default", "date"),
                                        Map.of("key", "options", "label", "Options", "type", "json")
                                )
                        ),
                        Map.of(
                                "actionKey", "dateTime:addToDate",
                                "name", "Add to a Date",
                                "description", "Add a specific amount of time to a date",
                                "configSchema", List.of(
                                        Map.of("key", "magnitude", "label", "Base Date", "type", "datetime", "required", true, "default", "{{now}}"),
                                        Map.of("key", "timeUnit", "label", "Time Unit", "type", "select", "required", true,
                                                "options", List.of("seconds", "minutes", "hours", "days", "weeks", "months", "years"), "default", "days"),
                                        Map.of("key", "duration", "label", "Duration Amount", "type", "number", "required", true, "default", 1),
                                        Map.of("key", "outputFieldName", "label", "Output Field Name", "type", "text", "default", "date")
                                )
                        ),
                        Map.of(
                                "actionKey", "dateTime:subtractFromDate",
                                "name", "Subtract From a Date",
                                "description", "Subtract a specific amount of time from a date",
                                "configSchema", List.of(
                                        Map.of("key", "magnitude", "label", "Base Date", "type", "datetime", "required", true, "default", "{{now}}"),
                                        Map.of("key", "timeUnit", "label", "Time Unit", "type", "select", "required", true,
                                                "options", List.of("seconds", "minutes", "hours", "days", "weeks", "months", "years"), "default", "days"),
                                        Map.of("key", "duration", "label", "Duration Amount", "type", "number", "required", true, "default", 1),
                                        Map.of("key", "outputFieldName", "label", "Output Field Name", "type", "text", "default", "date")
                                )
                        ),
                        Map.of(
                                "actionKey", "dateTime:formatDate",
                                "name", "Format a Date",
                                "description", "Format a date into a specific format string",
                                "configSchema", List.of(
                                        Map.of("key", "date", "label", "Date", "type", "datetime", "required", true, "default", "{{now}}"),
                                        Map.of("key", "format", "label", "Format Preset", "type", "select", "required", true,
                                                "options", List.of(
                                                        Map.of("value", "YYYY-MM-DD", "label", "YYYY-MM-DD (2026-08-21)"),
                                                        Map.of("value", "YYYY-MM-DDTHH:mm:ssZ", "label", "ISO 8601 (2026-08-21T11:30:00Z)"),
                                                        Map.of("value", "MM/DD/YYYY", "label", "MM/DD/YYYY (08/21/2026)"),
                                                        Map.of("value", "DD/MM/YYYY", "label", "DD/MM/YYYY (21/08/2026)"),
                                                        Map.of("value", "HH:mm:ss", "label", "Time Only (11:30:00)"),
                                                        Map.of("value", "X", "label", "Unix Timestamp in Seconds"),
                                                        Map.of("value", "x", "label", "Unix Timestamp in Milliseconds"),
                                                        Map.of("value", "custom", "label", "Custom Format String")
                                                ), "default", "YYYY-MM-DD"),
                                        Map.of("key", "customFormat", "label", "Custom Format", "type", "text", "placeholder", "YYYY-MM-DD HH:mm"),
                                        Map.of("key", "outputFieldName", "label", "Output Field Name", "type", "text", "default", "date")
                                )
                        ),
                        Map.of(
                                "actionKey", "dateTime:roundDate",
                                "name", "Round a Date",
                                "description", "Round a date up or down",
                                "configSchema", List.of(
                                        Map.of("key", "date", "label", "Date", "type", "datetime", "required", true, "default", "{{now}}"),
                                        Map.of("key", "mode", "label", "Rounding Mode", "type", "select", "required", true,
                                                "options", List.of("round", "floor", "ceil"), "default", "round"),
                                        Map.of("key", "toNearest", "label", "To Nearest", "type", "select",
                                                "options", List.of("minute", "hour", "day", "month"), "default", "hour"),
                                        Map.of("key", "outputFieldName", "label", "Output Field Name", "type", "text", "default", "date")
                                )
                        ),
                        Map.of(
                                "actionKey", "dateTime:getTimeBetweenDates",
                                "name", "Get Time Between Dates",
                                "description", "Calculate the difference between two dates",
                                "configSchema", List.of(
                                        Map.of("key", "startDate", "label", "Start Date", "type", "datetime", "required", true, "default", "{{now - 1d}}"),
                                        Map.of("key", "endDate", "label", "End Date", "type", "datetime", "required", true, "default", "{{now}}"),
                                        Map.of("key", "units", "label", "Units", "type", "select", "required", true,
                                                "options", List.of("seconds", "minutes", "hours", "days", "weeks", "months"), "default", "hours"),
                                        Map.of("key", "outputFieldName", "label", "Output Field Name", "type", "text", "default", "difference")
                                )
                        ),
                        Map.of(
                                "actionKey", "dateTime:extractDate",
                                "name", "Extract Part of a Date",
                                "description", "Extract a specific part (e.g., year, month) from a date",
                                "configSchema", List.of(
                                        Map.of("key", "date", "label", "Date", "type", "datetime", "required", true, "default", "{{now}}"),
                                        Map.of("key", "part", "label", "Part to Extract", "type", "select", "required", true,
                                                "options", List.of("year", "month", "day", "hour", "minute", "second", "weekday", "quarter"), "default", "year"),
                                        Map.of("key", "outputFieldName", "label", "Output Field Name", "type", "text", "default", "extractedPart")
                                )
                        )
                )
        ).credentialSchema(List.of()).category("data-transformation");
    }
}
