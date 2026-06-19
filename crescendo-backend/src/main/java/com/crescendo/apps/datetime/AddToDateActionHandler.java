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
@ActionMapping(appKey = "date-time", actionKey = "add-to-date")
public class AddToDateActionHandler implements ActionHandler {
    @Override
public ActionResult execute(ActionContext context) {
        try {
            Map<String, Object> config = context.configuration();
            ZoneId zone = DateTimeSupport.zone(config.get("timezone"));
            ZonedDateTime date = DateTimeSupport.parseDateTime(config.getOrDefault("date", "now"), zone);
            int amount = DateTimeSupport.intValue(config.get("amount"), 1);
            String unit = String.valueOf(config.getOrDefault("unit", "days")).toLowerCase();
            ZonedDateTime result = switch (unit) {
                case "seconds" -> date.plusSeconds(amount);
                case "minutes" -> date.plusMinutes(amount);
                case "hours" -> date.plusHours(amount);
                case "weeks" -> date.plusWeeks(amount);
                case "months" -> date.plusMonths(amount);
                case "years" -> date.plusYears(amount);
                default -> date.plusDays(amount);
            };
            return ActionResult.success(Map.of("date", DateTimeSupport.format(result, config.get("format"))));
        } catch (Exception e) {
            return ActionResult.failure("Add to Date failed: " + e.getMessage());
        }
    }
}
