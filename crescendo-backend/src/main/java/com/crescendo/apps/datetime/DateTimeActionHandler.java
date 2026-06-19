package com.crescendo.apps.datetime;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;


import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import java.util.Map;

@Component
@ActionMapping(appKey = "date-time", actionKey = "current-date")
public class DateTimeActionHandler implements ActionHandler {

    @Override
public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        
        String format = null;
        String timezone = "UTC";
        
        if (config != null) {
            if (config.containsKey("format") && config.get("format") != null) {
                format = String.valueOf(config.get("format"));
            }
            if (config.containsKey("timezone") && config.get("timezone") != null) {
                timezone = String.valueOf(config.get("timezone"));
            }
        }
        
        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(timezone);
        } catch (Exception e) {
            return ActionResult.failure("Invalid timezone: " + timezone);
        }

        ZonedDateTime now = ZonedDateTime.now(zoneId);
        
        if (format == null || format.isBlank()) {
            // Default to ISO
            return ActionResult.success(Map.of("date", now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
        }
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            return ActionResult.success(Map.of("date", now.format(formatter)));
        } catch (IllegalArgumentException e) {
            return ActionResult.failure("Invalid date format pattern: " + format);
        }
    }
}
