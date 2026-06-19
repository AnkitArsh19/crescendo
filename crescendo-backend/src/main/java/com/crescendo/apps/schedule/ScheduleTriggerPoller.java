package com.crescendo.apps.schedule;

import com.crescendo.execution.trigger.TriggerPoller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
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
                    // It fired! Provide a payload so the engine has some data
                    events.add(Map.of(
                        "id", UUID.randomUUID().toString(),
                        "timestamp", nextExecution.toInstant().toString(),
                        "type", "cron",
                        "expression", cronStr
                    ));
                    logger.info("[schedule-poller] Cron {} fired at {}", cronStr, nextExecution);
                }
            } else if (configuration.containsKey("minutes")) {
                long minutes = Long.parseLong(String.valueOf(configuration.get("minutes")));
                if (minutes < 1) minutes = 1;
                
                Instant nextExecution = lastPollTime.plus(Duration.ofMinutes(minutes));
                if (!nextExecution.isAfter(now)) {
                    events.add(Map.of(
                        "id", UUID.randomUUID().toString(),
                        "timestamp", now.toString(),
                        "type", "interval",
                        "intervalMinutes", minutes
                    ));
                    logger.info("[schedule-poller] Interval {}m fired", minutes);
                }
            }
        } catch (Exception e) {
            logger.warn("[schedule-poller] Failed to evaluate schedule configuration: {}", e.getMessage());
        }

        return events;
    }
}
