package com.crescendo.apps.wait;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

@Component
@ActionMapping(appKey = "wait", actionKey = "pause")
public class WaitActionHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(WaitActionHandler.class);

    @Override
public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        
        if (config == null) {
            return ActionResult.failure("Wait action requires configuration");
        }
        
        try {
            Instant resumeAt = resolveResumeAt(config);
            long seconds = Math.max(0, java.time.Duration.between(Instant.now(), resumeAt).getSeconds());
            
            if (seconds > 60) {
                logger.info("[wait] Suspending execution for {} seconds", seconds);
                throw new com.crescendo.execution.action.SuspendExecutionException(
                        resumeAt,
                        "Workflow paused for " + seconds + " seconds"
                );
            }
            
            logger.info("[wait] Pausing execution for {} seconds synchronously", seconds);
            Thread.sleep(seconds * 1000L);
            
            return ActionResult.success(context.inputData());
            
        } catch (IllegalArgumentException e) {
            return ActionResult.failure(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ActionResult.failure("Wait interrupted");
        }
    }

    private Instant resolveResumeAt(Map<String, Object> config) {
        String mode = String.valueOf(config.getOrDefault("mode", "duration"));
        if ("until".equalsIgnoreCase(mode)) {
            Object resumeAt = config.get("resumeAt");
            if (resumeAt == null || String.valueOf(resumeAt).isBlank()) {
                throw new IllegalArgumentException("Wait Until mode requires resumeAt");
            }
            ZoneId zone = ZoneId.of(String.valueOf(config.getOrDefault("timezone", "UTC")));
            return parseDateTime(String.valueOf(resumeAt), zone).toInstant();
        }

        Object secondsObj = config.get("seconds");
        if (secondsObj == null) {
            throw new IllegalArgumentException("Wait Duration mode requires seconds");
        }
        long seconds;
        try {
            seconds = Long.parseLong(String.valueOf(secondsObj));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'seconds' must be a valid number");
        }
        if (seconds < 0) {
            throw new IllegalArgumentException("Wait seconds cannot be negative");
        }
        return Instant.now().plusSeconds(seconds);
    }

    private ZonedDateTime parseDateTime(String value, ZoneId zone) {
        try {
            return ZonedDateTime.parse(value);
        } catch (Exception ignored) {
        }
        try {
            return Instant.parse(value).atZone(zone);
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(value).atZone(zone);
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(value).atStartOfDay(zone);
        } catch (Exception ignored) {
        }
        throw new IllegalArgumentException("resumeAt must be an ISO date/time, local date/time, or date");
    }
}
