package com.crescendo.apps.pomodoro;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ActionMapping(appKey = "pomodoro", actionKey = "calculate-end-time")
public class PomodoroCalculateEndTimeHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(PomodoroCalculateEndTimeHandler.class);


    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();

        Object minutesObj = config.get("minutes");
        if (minutesObj == null) {
            return ActionResult.failure("'minutes' is required");
        }

        int minutes;
        try {
            minutes = Integer.parseInt(minutesObj.toString());
        } catch (NumberFormatException e) {
            return ActionResult.failure("'minutes' must be a valid integer");
        }

        if (minutes <= 0) {
            return ActionResult.failure("'minutes' must be positive");
        }

        Instant now = Instant.now();
        Instant endTime = now.plus(minutes, ChronoUnit.MINUTES);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                .withZone(ZoneId.systemDefault());

        Map<String, Object> output = new HashMap<>();
        output.put("startTime", now.toString());
        output.put("endTime", endTime.toString());
        output.put("startTimeFormatted", formatter.format(now));
        output.put("endTimeFormatted", formatter.format(endTime));
        output.put("durationMinutes", minutes);

        logger.info("[pomodoro] End time calculated, durationMinutes={}", minutes);
        return ActionResult.success(output);
    }
}
