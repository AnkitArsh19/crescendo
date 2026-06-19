package com.crescendo.apps.datetime;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DateTimeApp implements AppDefinition {
    @Override
    public App toApp() {
        var unitOptions = List.of(
                Map.of("value", "seconds", "label", "Seconds"),
                Map.of("value", "minutes", "label", "Minutes"),
                Map.of("value", "hours", "label", "Hours"),
                Map.of("value", "days", "label", "Days"),
                Map.of("value", "weeks", "label", "Weeks"),
                Map.of("value", "months", "label", "Months"),
                Map.of("value", "years", "label", "Years")
        );
        var roundUnitOptions = List.of(
                Map.of("value", "second", "label", "Second"),
                Map.of("value", "minute", "label", "Minute"),
                Map.of("value", "hour", "label", "Hour"),
                Map.of("value", "day", "label", "Day"),
                Map.of("value", "week", "label", "Week"),
                Map.of("value", "month", "label", "Month"),
                Map.of("value", "year", "label", "Year")
        );
        var dateField = Map.of("key", "date", "label", "Date", "type", "text", "required", false,
                "placeholder", "now or {{steps.1.date}}", "helpText", "ISO date/time, epoch timestamp, or now");
        var timezoneField = Map.of("key", "timezone", "label", "Timezone", "type", "text", "required", false,
                "placeholder", "UTC", "helpText", "e.g. UTC, America/New_York, Asia/Kolkata");
        var formatField = Map.of("key", "format", "label", "Output Format", "type", "text", "required", false,
                "placeholder", "yyyy-MM-dd'T'HH:mm:ssXXX", "helpText", "Java DateTimeFormatter pattern. Leave blank for ISO.");

        return new App("date-time", "Date & Time", "Generate and format dates and times",
                "/icons/date-time.svg", AuthType.NONE,
                List.of(),
                List.of(
                    Map.of("actionKey", "current-date", "name", "Get Current Date",
                        "description", "Returns the current date and time",
                        "configSchema", List.of(
                            Map.of("key", "format", "label", "Format (Optional)", "type", "text", "required", false,
                                   "placeholder", "yyyy-MM-dd'T'HH:mm:ss'Z'", "helpText", "Java DateTimeFormatter pattern. Defaults to ISO-8601."),
                            Map.of("key", "timezone", "label", "Timezone", "type", "text", "required", false,
                                   "placeholder", "UTC", "helpText", "e.g. UTC, America/New_York, Asia/Kolkata")
                        )),
                    Map.of("actionKey", "format-date", "name", "Format Date",
                        "description", "Format an existing date string",
                        "configSchema", List.of(
                            Map.of("key", "date", "label", "Input Date", "type", "text", "required", true,
                                   "placeholder", "{{steps.1.created_at}}", "helpText", "The date string to format"),
                            Map.of("key", "format", "label", "Output Format", "type", "text", "required", true,
                                   "placeholder", "yyyy-MM-dd", "helpText", "Java DateTimeFormatter pattern."),
                            Map.of("key", "timezone", "label", "Target Timezone", "type", "text", "required", false,
                                   "placeholder", "UTC", "helpText", "e.g. UTC, America/New_York")
                        )),
                    Map.of("actionKey", "add-to-date", "name", "Add to Date",
                        "description", "Add seconds, minutes, hours, days, weeks, months, or years",
                        "configSchema", List.of(
                            dateField,
                            Map.of("key", "amount", "label", "Amount", "type", "number", "required", true,
                                   "placeholder", "3", "helpText", "Amount to add"),
                            Map.of("key", "unit", "label", "Unit", "type", "select", "required", true,
                                   "options", unitOptions, "helpText", "Time unit"),
                            timezoneField,
                            formatField
                        )),
                    Map.of("actionKey", "subtract-from-date", "name", "Subtract From Date",
                        "description", "Subtract seconds, minutes, hours, days, weeks, months, or years",
                        "configSchema", List.of(
                            dateField,
                            Map.of("key", "amount", "label", "Amount", "type", "number", "required", true,
                                   "placeholder", "7", "helpText", "Amount to subtract"),
                            Map.of("key", "unit", "label", "Unit", "type", "select", "required", true,
                                   "options", unitOptions, "helpText", "Time unit"),
                            timezoneField,
                            formatField
                        )),
                    Map.of("actionKey", "round-date", "name", "Round Date",
                        "description", "Round a date up or down to a time boundary",
                        "configSchema", List.of(
                            dateField,
                            Map.of("key", "mode", "label", "Mode", "type", "select", "required", true,
                                   "options", List.of(Map.of("value", "down", "label", "Round Down"), Map.of("value", "up", "label", "Round Up")),
                                   "helpText", "Direction to round"),
                            Map.of("key", "unit", "label", "Nearest", "type", "select", "required", true,
                                   "options", roundUnitOptions, "helpText", "Boundary to round to"),
                            timezoneField,
                            formatField
                        )),
                    Map.of("actionKey", "extract-part", "name", "Extract Date Part",
                        "description", "Extract year, month, week, day, hour, minute, second, or timestamp",
                        "configSchema", List.of(
                            dateField,
                            Map.of("key", "part", "label", "Part", "type", "select", "required", true,
                                   "options", List.of(
                                       Map.of("value", "year", "label", "Year"),
                                       Map.of("value", "month", "label", "Month"),
                                       Map.of("value", "week", "label", "ISO Week"),
                                       Map.of("value", "day", "label", "Day"),
                                       Map.of("value", "weekday", "label", "Weekday"),
                                       Map.of("value", "hour", "label", "Hour"),
                                       Map.of("value", "minute", "label", "Minute"),
                                       Map.of("value", "second", "label", "Second"),
                                       Map.of("value", "timestamp", "label", "Timestamp (ms)")
                                   ), "helpText", "Date part to extract"),
                            timezoneField
                        )),
                    Map.of("actionKey", "time-between", "name", "Time Between Dates",
                        "description", "Calculate the difference between two dates",
                        "configSchema", List.of(
                            Map.of("key", "startDate", "label", "Start Date", "type", "text", "required", true,
                                   "placeholder", "{{steps.1.start}}", "helpText", "ISO date/time, epoch timestamp, or now"),
                            Map.of("key", "endDate", "label", "End Date", "type", "text", "required", true,
                                   "placeholder", "now", "helpText", "ISO date/time, epoch timestamp, or now"),
                            timezoneField
                        ))
                )
        ).credentialSchema(List.of()).category("core").helpUrl("");
    }
}
