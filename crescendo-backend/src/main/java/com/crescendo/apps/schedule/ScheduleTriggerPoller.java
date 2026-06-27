package com.crescendo.apps.schedule;

import com.crescendo.execution.trigger.TriggerPoller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

// import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
// import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ScheduleTriggerPoller implements TriggerPoller {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleTriggerPoller.class);

    @Override
    public boolean supports(String appKey, String triggerKey) {
        return "schedule".equals(appKey);
    }

    @Override
    public List<Map<String, Object>> poll(Map<String, Object> credentials, Map<String, Object> configuration, Instant lastPollTime) {
        List<Map<String, Object>> events = new ArrayList<>();
        Instant now = Instant.now();

        if (configuration == null) return events;

        try {
            if (configuration.containsKey("cronExpression")) {
                String cronStr = String.valueOf(configuration.get("cronExpression"));
                CronExpression cron = CronExpression.parse(cronStr);
                
                // Determine if the cron should have fired between lastPollTime and now
                ZonedDateTime lastPollZoned = lastPollTime.atZone(ZoneId.of("UTC"));
                ZonedDateTime nextExecution = cron.next(lastPollZoned);
                
                if (nextExecution != null && !nextExecution.toInstant().isAfter(now)) {
                    events.add(buildN8nPayload(nextExecution, cronStr, "cron"));
                    logger.info("[schedule-poller] Cron {} fired at {}", cronStr, nextExecution);
                }
            } else if (configuration.containsKey("minutes") || configuration.containsKey("interval")) {
                long intervalValue = configuration.containsKey("interval") ? 
                    Long.parseLong(String.valueOf(configuration.get("interval"))) : 
                    Long.parseLong(String.valueOf(configuration.get("minutes")));
                
                if (intervalValue < 1) intervalValue = 1;
                
                String unit = String.valueOf(configuration.getOrDefault("unit", "minutes")).toLowerCase();
                
                ZonedDateTime lastPollZoned = lastPollTime.atZone(ZoneId.of("UTC"));
                ZonedDateTime nextExecution = switch (unit) {
                    case "seconds" -> lastPollZoned.plusSeconds(intervalValue);
                    case "hours" -> lastPollZoned.plusHours(intervalValue);
                    case "days" -> lastPollZoned.plusDays(intervalValue);
                    case "weeks" -> lastPollZoned.plusWeeks(intervalValue);
                    case "months" -> lastPollZoned.plusMonths(intervalValue);
                    default -> lastPollZoned.plusMinutes(intervalValue);
                };
                
                if (!nextExecution.toInstant().isAfter(now)) {
                    events.add(buildN8nPayload(ZonedDateTime.now(ZoneId.of("UTC")), intervalValue + " " + unit, "interval"));
                    logger.info("[schedule-poller] Interval {} {} fired", intervalValue, unit);
                }
            }
        } catch (Exception e) {
            logger.warn("[schedule-poller] Failed to evaluate schedule configuration: {}", e.getMessage());
        }

        return events;
    }

    private Map<String, Object> buildN8nPayload(ZonedDateTime time, String rule, String type) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("timestamp", DateTimeFormatter.ISO_INSTANT.format(time));
        
        String daySuffix = getDaySuffix(time.getDayOfMonth());
        payload.put("Readable date", DateTimeFormatter.ofPattern("MMMM d'" + daySuffix + "' yyyy, h:mm:ss a").format(time));
        payload.put("Readable time", DateTimeFormatter.ofPattern("h:mm:ss a").format(time));
        payload.put("Day of week", DateTimeFormatter.ofPattern("EEEE").format(time));
        payload.put("Year", DateTimeFormatter.ofPattern("yyyy").format(time));
        payload.put("Month", DateTimeFormatter.ofPattern("MMMM").format(time));
        payload.put("Day of month", DateTimeFormatter.ofPattern("dd").format(time));
        payload.put("Hour", DateTimeFormatter.ofPattern("HH").format(time));
        payload.put("Minute", DateTimeFormatter.ofPattern("mm").format(time));
        payload.put("Second", DateTimeFormatter.ofPattern("ss").format(time));
        payload.put("Timezone", time.getZone().getId() + " (UTC" + DateTimeFormatter.ofPattern("xxx").format(time) + ")");
        
        // Crescendo internal fields
        payload.put("_id", UUID.randomUUID().toString());
        payload.put("_type", type);
        payload.put("_rule", rule);
        
        return payload;
    }

    private String getDaySuffix(int day) {
        if (day >= 11 && day <= 13) return "th";
        return switch (day % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }
}
