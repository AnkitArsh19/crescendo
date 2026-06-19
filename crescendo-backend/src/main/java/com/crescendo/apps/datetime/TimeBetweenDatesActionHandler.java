package com.crescendo.apps.datetime;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ActionMapping(appKey = "date-time", actionKey = "time-between")
public class TimeBetweenDatesActionHandler implements ActionHandler {
    @Override
public ActionResult execute(ActionContext context) {
        try {
            Map<String, Object> config = context.configuration();
            ZoneId zone = DateTimeSupport.zone(config.get("timezone"));
            ZonedDateTime start = DateTimeSupport.parseDateTime(config.get("startDate"), zone);
            ZonedDateTime end = DateTimeSupport.parseDateTime(config.get("endDate"), zone);
            Duration duration = Duration.between(start, end);
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("milliseconds", duration.toMillis());
            output.put("seconds", duration.toSeconds());
            output.put("minutes", duration.toMinutes());
            output.put("hours", duration.toHours());
            output.put("days", duration.toDays());
            output.put("months", ChronoUnit.MONTHS.between(start, end));
            output.put("years", ChronoUnit.YEARS.between(start, end));
            return ActionResult.success(output);
        } catch (Exception e) {
            return ActionResult.failure("Time Between Dates failed: " + e.getMessage());
        }
    }
}
