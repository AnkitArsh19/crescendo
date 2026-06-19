package com.crescendo.apps.datetime;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.WeekFields;
import java.util.Map;

@Component
@ActionMapping(appKey = "date-time", actionKey = "extract-part")
public class ExtractDatePartActionHandler implements ActionHandler {
    @Override
public ActionResult execute(ActionContext context) {
        try {
            Map<String, Object> config = context.configuration();
            ZoneId zone = DateTimeSupport.zone(config.get("timezone"));
            ZonedDateTime date = DateTimeSupport.parseDateTime(config.getOrDefault("date", "now"), zone);
            String part = String.valueOf(config.getOrDefault("part", "day")).toLowerCase();
            Object value = switch (part) {
                case "year" -> date.getYear();
                case "month" -> date.getMonthValue();
                case "day" -> date.getDayOfMonth();
                case "weekday" -> date.getDayOfWeek().getValue();
                case "week" -> date.get(WeekFields.ISO.weekOfWeekBasedYear());
                case "hour" -> date.getHour();
                case "minute" -> date.getMinute();
                case "second" -> date.getSecond();
                case "timestamp" -> date.toInstant().toEpochMilli();
                default -> date.getDayOfMonth();
            };
            return ActionResult.success(Map.of("part", part, "value", value));
        } catch (Exception e) {
            return ActionResult.failure("Extract Date Part failed: " + e.getMessage());
        }
    }
}
