package com.crescendo.apps.pomodoro;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ActionMapping(appKey = "pomodoro", actionKey = "create-timer")
public class PomodoroCreateTimerHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(PomodoroCreateTimerHandler.class);


    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();

        int workMinutes = getInt(config, "workMinutes", 25);
        int breakMinutes = getInt(config, "breakMinutes", 5);
        int longBreakMinutes = getInt(config, "longBreakMinutes", 15);
        int sessionsBeforeLongBreak = getInt(config, "sessionsBeforeLongBreak", 4);
        String taskLabel = config.get("taskLabel") != null ? config.get("taskLabel").toString() : "Pomodoro Session";

        Instant now = Instant.now();
        Instant workEnd = now.plus(workMinutes, ChronoUnit.MINUTES);

        Map<String, Object> timerConfig = new HashMap<>();
        timerConfig.put("taskLabel", taskLabel);
        timerConfig.put("workMinutes", workMinutes);
        timerConfig.put("breakMinutes", breakMinutes);
        timerConfig.put("longBreakMinutes", longBreakMinutes);
        timerConfig.put("sessionsBeforeLongBreak", sessionsBeforeLongBreak);
        timerConfig.put("startedAt", now.toString());
        timerConfig.put("workEndsAt", workEnd.toString());
        timerConfig.put("totalCycleMinutes", (workMinutes + breakMinutes) * sessionsBeforeLongBreak + longBreakMinutes);

        logger.info("[pomodoro] Timer created, workMinutes={}", workMinutes);
        return ActionResult.success(timerConfig);
    }

    private int getInt(Map<String, Object> config, String key, int defaultValue) {
        if (config.containsKey(key)) {
            try {
                return Integer.parseInt(config.get(key).toString());
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }
}
