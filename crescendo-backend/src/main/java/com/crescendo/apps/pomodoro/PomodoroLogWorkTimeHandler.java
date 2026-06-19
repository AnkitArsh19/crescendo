package com.crescendo.apps.pomodoro;

import com.crescendo.execution.action.*;
import java.time.Instant;
import java.util.*;

/**
 * Logs work time — a virtual utility that records a completed work session.
 */
@ActionMapping(appKey = "pomodoro", actionKey = "log-work-time")
public class PomodoroLogWorkTimeHandler implements ActionHandler {

    @Override
public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String duration = config.get("durationMinutes") != null ? config.get("durationMinutes").toString() : null;
        if (duration == null) return ActionResult.failure("'durationMinutes' is required");

        String task = config.get("task") != null ? config.get("task").toString() : "Unnamed session";

        try {
            int minutes = Integer.parseInt(duration);
            Map<String, Object> out = new HashMap<>();
            out.put("provider", "pomodoro");
            out.put("action", "log-work-time");
            out.put("task", task);
            out.put("durationMinutes", minutes);
            out.put("loggedAt", Instant.now().toString());
            out.put("message", "Logged " + minutes + " minutes of work on: " + task);
            return ActionResult.success(out);
        } catch (NumberFormatException e) {
            return ActionResult.failure("Invalid duration: " + duration);
        }
    }
}
