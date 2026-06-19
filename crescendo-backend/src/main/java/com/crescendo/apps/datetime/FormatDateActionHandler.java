package com.crescendo.apps.datetime;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;


import java.time.ZoneId;
import java.time.ZonedDateTime;

import java.util.Map;

@Component
@ActionMapping(appKey = "date-time", actionKey = "format-date")
public class FormatDateActionHandler implements ActionHandler {

    @Override
public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        
        if (config == null || !config.containsKey("date") || !config.containsKey("format")) {
            return ActionResult.failure("Format Date requires both 'date' and 'format' in configuration");
        }
        
        String inputDate = String.valueOf(config.get("date"));
        String format = String.valueOf(config.get("format"));
        String timezone = config.containsKey("timezone") && config.get("timezone") != null 
                ? String.valueOf(config.get("timezone")) 
                : "UTC";
                
        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(timezone);
        } catch (Exception e) {
            return ActionResult.failure("Invalid timezone: " + timezone);
        }

        try {
            ZonedDateTime zdt = DateTimeSupport.parseDateTime(inputDate, zoneId);
            return ActionResult.success(Map.of("formattedDate", DateTimeSupport.format(zdt, format)));
            
        } catch (Exception e) {
            return ActionResult.failure("Failed to format date: " + e.getMessage());
        }
    }
}
