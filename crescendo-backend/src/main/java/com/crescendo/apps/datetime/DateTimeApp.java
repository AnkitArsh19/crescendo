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
                                        Map.of("key", "magnitude", "label", "Date", "type", "text", "required", true),
                                        Map.of("key", "timeUnit", "label", "Time Unit", "type", "text", "required", true),
                                        Map.of("key", "duration", "label", "Duration", "type", "number", "required", true),
                                        Map.of("key", "outputFieldName", "label", "Output Field Name", "type", "text", "default", "date")
                                )
                        ),
                        Map.of(
                                "actionKey", "dateTime:subtractFromDate",
                                "name", "Subtract From a Date",
                                "description", "Subtract a specific amount of time from a date",
                                "configSchema", List.of(
                                        Map.of("key", "magnitude", "label", "Date", "type", "text", "required", true),
                                        Map.of("key", "timeUnit", "label", "Time Unit", "type", "text", "required", true),
                                        Map.of("key", "duration", "label", "Duration", "type", "number", "required", true),
                                        Map.of("key", "outputFieldName", "label", "Output Field Name", "type", "text", "default", "date")
                                )
                        ),
                        Map.of(
                                "actionKey", "dateTime:formatDate",
                                "name", "Format a Date",
                                "description", "Format a date into a specific format string",
                                "configSchema", List.of(
                                        Map.of("key", "date", "label", "Date", "type", "text", "required", true),
                                        Map.of("key", "format", "label", "Format", "type", "text", "required", true),
                                        Map.of("key", "customFormat", "label", "Custom Format", "type", "text"),
                                        Map.of("key", "outputFieldName", "label", "Output Field Name", "type", "text", "default", "date"),
                                        Map.of("key", "options", "label", "Options", "type", "json")
                                )
                        ),
                        Map.of(
                                "actionKey", "dateTime:roundDate",
                                "name", "Round a Date",
                                "description", "Round a date up or down",
                                "configSchema", List.of(
                                        Map.of("key", "date", "label", "Date", "type", "text", "required", true),
                                        Map.of("key", "mode", "label", "Mode", "type", "text", "required", true),
                                        Map.of("key", "toNearest", "label", "To Nearest", "type", "text"),
                                        Map.of("key", "to", "label", "To", "type", "text"),
                                        Map.of("key", "outputFieldName", "label", "Output Field Name", "type", "text", "default", "date")
                                )
                        ),
                        Map.of(
                                "actionKey", "dateTime:getTimeBetweenDates",
                                "name", "Get Time Between Dates",
                                "description", "Calculate the difference between two dates",
                                "configSchema", List.of(
                                        Map.of("key", "startDate", "label", "Start Date", "type", "text", "required", true),
                                        Map.of("key", "endDate", "label", "End Date", "type", "text", "required", true),
                                        Map.of("key", "units", "label", "Units", "type", "text", "required", true),
                                        Map.of("key", "outputFieldName", "label", "Output Field Name", "type", "text", "default", "date"),
                                        Map.of("key", "options", "label", "Options", "type", "json")
                                )
                        ),
                        Map.of(
                                "actionKey", "dateTime:extractDate",
                                "name", "Extract Part of a Date",
                                "description", "Extract a specific part (e.g., year, month) from a date",
                                "configSchema", List.of(
                                        Map.of("key", "date", "label", "Date", "type", "text", "required", true),
                                        Map.of("key", "part", "label", "Part", "type", "text", "required", true),
                                        Map.of("key", "outputFieldName", "label", "Output Field Name", "type", "text", "default", "date")
                                )
                        )
                )
        ).credentialSchema(List.of()).category("data-transformation");
    }
}
