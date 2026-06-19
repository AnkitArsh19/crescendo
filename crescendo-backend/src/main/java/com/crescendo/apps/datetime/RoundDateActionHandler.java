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
@ActionMapping(appKey = "date-time", actionKey = "round-date")
public class RoundDateActionHandler implements ActionHandler {
    @Override
public ActionResult execute(ActionContext context) {
        try {
            Map<String, Object> config = context.configuration();
            ZoneId zone = DateTimeSupport.zone(config.get("timezone"));
            ZonedDateTime date = DateTimeSupport.parseDateTime(config.getOrDefault("date", "now"), zone);
            String unit = String.valueOf(config.getOrDefault("unit", "day")).toLowerCase();
            String mode = String.valueOf(config.getOrDefault("mode", "down")).toLowerCase();
            ZonedDateTime rounded = roundDown(date, unit);
            if ("up".equals(mode) && !rounded.equals(date)) {
                rounded = switch (unit) {
                    case "second" -> rounded.plusSeconds(1);
                    case "minute" -> rounded.plusMinutes(1);
                    case "hour" -> rounded.plusHours(1);
                    case "week" -> rounded.plusWeeks(1);
                    case "month" -> rounded.plusMonths(1);
                    case "year" -> rounded.plusYears(1);
                    default -> rounded.plusDays(1);
                };
            }
            return ActionResult.success(Map.of("date", DateTimeSupport.format(rounded, config.get("format"))));
        } catch (Exception e) {
            return ActionResult.failure("Round Date failed: " + e.getMessage());
        }
    }

    private ZonedDateTime roundDown(ZonedDateTime date, String unit) {
        return switch (unit) {
            case "second" -> date.withNano(0);
            case "minute" -> date.withSecond(0).withNano(0);
            case "hour" -> date.withMinute(0).withSecond(0).withNano(0);
            case "week" -> date.minusDays(date.getDayOfWeek().getValue() - 1L).toLocalDate().atStartOfDay(date.getZone());
            case "month" -> date.withDayOfMonth(1).toLocalDate().atStartOfDay(date.getZone());
            case "year" -> date.withDayOfYear(1).toLocalDate().atStartOfDay(date.getZone());
            default -> date.toLocalDate().atStartOfDay(date.getZone());
        };
    }
}
