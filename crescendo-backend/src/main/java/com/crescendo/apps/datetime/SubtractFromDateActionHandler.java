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
@ActionMapping(appKey = "date-time", actionKey = "subtract-from-date")
public class SubtractFromDateActionHandler implements ActionHandler {
    @Override
public ActionResult execute(ActionContext context) {
        try {
            Map<String, Object> config = context.configuration();
            ZoneId zone = DateTimeSupport.zone(config.get("timezone"));
            ZonedDateTime date = DateTimeSupport.parseDateTime(config.getOrDefault("date", "now"), zone);
            int amount = DateTimeSupport.intValue(config.get("amount"), 1);
            String unit = String.valueOf(config.getOrDefault("unit", "days")).toLowerCase();
            ZonedDateTime result = switch (unit) {
                case "seconds" -> date.minusSeconds(amount);
                case "minutes" -> date.minusMinutes(amount);
                case "hours" -> date.minusHours(amount);
                case "weeks" -> date.minusWeeks(amount);
                case "months" -> date.minusMonths(amount);
                case "years" -> date.minusYears(amount);
                default -> date.minusDays(amount);
            };
            return ActionResult.success(Map.of("date", DateTimeSupport.format(result, config.get("format"))));
        } catch (Exception e) {
            return ActionResult.failure("Subtract from Date failed: " + e.getMessage());
        }
    }
}
