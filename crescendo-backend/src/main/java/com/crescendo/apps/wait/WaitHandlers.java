package com.crescendo.apps.wait;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.SuspendExecutionException;
import com.crescendo.execution.suspension.WorkflowSuspensionService;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Wait action handler — suspends workflow run until duration or specific time elapses.
 */
@Component
public class WaitHandlers {

    private final WorkflowSuspensionService suspensionService;

    public WaitHandlers(WorkflowSuspensionService suspensionService) {
        this.suspensionService = suspensionService;
    }

    @ActionMapping(appKey = "wait", actionKey = "wait:wait")
    public Object wait(ActionContext context) throws Exception {
        String resume = context.getString("resume");
        if (resume == null || resume.isBlank()) resume = "timeInterval";

        Instant resumeAt;
        if ("specificTime".equalsIgnoreCase(resume)) {
            String dtStr = context.getString("dateTime");
            try {
                resumeAt = dtStr != null ? Instant.parse(dtStr) : Instant.now().plus(Duration.ofMinutes(1));
            } catch (Exception e) {
                resumeAt = Instant.now().plus(Duration.ofMinutes(1));
            }
        } else {
            int amount = context.getInt("amount", 1);
            String unit = context.getString("unit");
            if (unit == null || unit.isBlank()) unit = "seconds";
            Duration duration = switch (unit.toLowerCase().trim()) {
                case "seconds", "second", "s" -> Duration.ofSeconds(amount);
                case "minutes", "minute", "m" -> Duration.ofMinutes(amount);
                case "hours", "hour", "h"     -> Duration.ofHours(amount);
                case "days", "day", "d"       -> Duration.ofDays(amount);
                default                       -> Duration.ofSeconds(amount);
            };
            resumeAt = Instant.now().plus(duration);
        }

        if (context.workflowRunId() == null) {
            return Map.of(
                "status", "waiting_preview",
                "resumeAt", resumeAt.toString(),
                "resume_mode", resume
            );
        }

        String resumeToken = UUID.randomUUID().toString();
        String correlationKey = "wait:" + context.workflowRunId() + ":" + context.stepId();
        suspensionService.suspend(context.workflowRunId(), context.stepId(), correlationKey, resumeToken, resumeAt);

        throw new SuspendExecutionException(
                resumeAt,
                resumeToken,
                Map.of(
                    "status", "waiting",
                    "resumeAt", resumeAt.toString(),
                    "resume_mode", resume
                ),
                "Waiting until " + resumeAt
        );
    }
}
