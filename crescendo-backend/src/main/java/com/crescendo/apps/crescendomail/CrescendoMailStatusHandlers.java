package com.crescendo.apps.crescendomail;

import com.crescendo.emailservice.email_log.EmailLog;
import com.crescendo.emailservice.email_log.EmailLogRepository;
import com.crescendo.enums.EmailStatus;
import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import com.crescendo.execution.action.SuspendExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.crescendo.apps.crescendomail.CrescendoMailSendHandlers.blank;
import static com.crescendo.apps.crescendomail.CrescendoMailSendHandlers.resolveUserId;

/**
 * Status action handlers for crescendomail.
 *
 * <p>Action keys:
 * <ul>
 *   <li>{@code crescendomail:get-status}     — fetch the current delivery status of a specific send
 *   <li>{@code crescendomail:wait-for-event} — suspend the workflow run until a named email event
 *                                              occurs on a specific send, or a timeout elapses
 * </ul>
 *
 * <p>{@code wait-for-event} is the action-level surface of {@link com.crescendo.execution.suspension.WorkflowSuspensionService}
 * (built in Phase 4). In this Phase 1 scaffold it suspends using a correlation key; the
 * SuspensionService wires the resume side in Phase 4.
 */
public class CrescendoMailStatusHandlers {

    /** Valid event types a caller can wait for. */
    private static final Set<String> VALID_EVENTS = Set.of("delivered", "opened", "clicked", "bounced");

    // ─────────────────────────────────────────────────────────────────────────
    // get-status
    // ─────────────────────────────────────────────────────────────────────────

    @Component
    @ActionMapping(appKey = "crescendomail", actionKey = "get-status")
    public static class GetStatusHandler implements ActionHandler {

        private static final Logger log = LoggerFactory.getLogger(GetStatusHandler.class);

        private final EmailLogRepository emailLogRepo;

        public GetStatusHandler(EmailLogRepository emailLogRepo) {
            this.emailLogRepo = emailLogRepo;
        }

        @Override
        public ActionResult execute(ActionContext ctx) {
            String emailIdStr = ctx.getString("emailId");
            if (blank(emailIdStr)) return ActionResult.failure("crescendomail:get-status requires 'emailId'");

            UUID emailId;
            try {
                emailId = UUID.fromString(emailIdStr);
            } catch (IllegalArgumentException e) {
                return ActionResult.failure("Invalid emailId format");
            }

            UUID userId = resolveUserId(ctx);
            EmailLog emailLog = emailLogRepo.findById(emailId).orElse(null);

            if (emailLog == null || !emailLog.getUserId().equals(userId)) {
                return ActionResult.failure("Email not found: " + emailId);
            }

            log.debug("[crescendomail:get-status] emailId={} status={}", emailId, emailLog.getStatus());
            return ActionResult.success(buildStatusOutput(emailLog));
        }

        private static Map<String, Object> buildStatusOutput(EmailLog log) {
            Map<String, Object> out = new java.util.HashMap<>();
            out.put("emailId",           log.getId().toString());
            out.put("status",            log.getStatus().name());
            out.put("to",                log.getToAddress());
            out.put("subject",           log.getSubject());
            out.put("openCount",         log.getOpenCount());
            out.put("clickCount",        log.getClickCount());
            out.put("sentAt",            log.getSentAt()    != null ? log.getSentAt().toString()    : null);
            out.put("openedAt",          log.getOpenedAt()  != null ? log.getOpenedAt().toString()  : null);
            out.put("providerMessageId", log.getProviderMessageId());
            out.put("error",             log.getError());
            return out;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // wait-for-event
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Suspends the workflow run until a named email event occurs on a specific send,
     * or until the configured timeout elapses.
     *
     * <p>Config:
     * <ul>
     *   <li>{@code emailId}       — UUID of the send to watch (required)
     *   <li>{@code event}         — one of: delivered, opened, clicked, bounced (required)
     *   <li>{@code timeoutHours}  — resume with TIMEOUT status after N hours (default: 24)
     * </ul>
     *
     * <p>The correlation key used to resume this suspension is:
     * {@code "crescendomail:{emailId}:{event}"} — email domain event listeners
     * call {@code WorkflowSuspensionService.resume(correlationKey, payload)} in Phase 4.
     */
    @Component
    @ActionMapping(appKey = "crescendomail", actionKey = "wait-for-event")
    public static class WaitForEventHandler implements ActionHandler {

        private static final Logger log = LoggerFactory.getLogger(WaitForEventHandler.class);
        private static final int DEFAULT_TIMEOUT_HOURS = 24;

        private final EmailLogRepository emailLogRepo;
        private final com.crescendo.execution.suspension.WorkflowSuspensionService suspensionService;

        public WaitForEventHandler(EmailLogRepository emailLogRepo, com.crescendo.execution.suspension.WorkflowSuspensionService suspensionService) {
            this.emailLogRepo = emailLogRepo;
            this.suspensionService = suspensionService;
        }

        @Override
        public ActionResult execute(ActionContext ctx) {
            String emailIdStr = ctx.getString("emailId");
            String event      = ctx.getString("event");

            if (blank(emailIdStr)) return ActionResult.failure("crescendomail:wait-for-event requires 'emailId'");
            if (blank(event))      return ActionResult.failure("crescendomail:wait-for-event requires 'event'");

            String normalizedEvent = event.toLowerCase().trim();
            if (!VALID_EVENTS.contains(normalizedEvent)) {
                return ActionResult.failure("crescendomail:wait-for-event 'event' must be one of: " + VALID_EVENTS);
            }

            UUID emailId;
            try {
                emailId = UUID.fromString(emailIdStr);
            } catch (IllegalArgumentException e) {
                return ActionResult.failure("Invalid emailId format");
            }

            int timeoutHours = ctx.getInt("timeoutHours", DEFAULT_TIMEOUT_HOURS);
            Instant resumeAt = Instant.now().plus(Duration.ofHours(timeoutHours));

            // Correlation key: resolved by WorkflowSuspensionService.resume() in Phase 4
            // when email domain events fire with matching emailId.
            String correlationKey = "crescendomail:" + emailId + ":" + normalizedEvent;

            log.info("[crescendomail:wait-for-event] Suspending run {} waiting for {} on email {}",
                    ctx.workflowRunId(), normalizedEvent, emailId);

            // If the event already happened (email already in desired status), return immediately
            EmailLog emailLog = emailLogRepo.findById(emailId).orElse(null);
            if (emailLog != null) {
                EmailStatus status = emailLog.getStatus();
                boolean alreadyDone = switch (normalizedEvent) {
                    case "delivered" -> status == EmailStatus.DELIVERED;
                    case "opened"    -> emailLog.getOpenCount() > 0;
                    case "clicked"   -> emailLog.getClickCount() > 0;
                    case "bounced"   -> status == EmailStatus.BOUNCED;
                    default          -> false;
                };
                if (alreadyDone) {
                    log.info("[crescendomail:wait-for-event] Event already occurred — continuing immediately");
                    return ActionResult.success(Map.of(
                            "emailId", emailId.toString(),
                            "event",   normalizedEvent,
                            "result",  "already_occurred"
                    ));
                }
            }

            String resumeToken = UUID.randomUUID().toString();
            suspensionService.suspend(ctx.workflowRunId(), ctx.stepId(), correlationKey, resumeToken, resumeAt);

            // Suspend — resume via WorkflowSuspensionService.resume(correlationKey, payload) in Phase 4
            throw new SuspendExecutionException(
                    resumeAt,
                    resumeToken,
                    Map.of(
                        "emailId",        emailId.toString(),
                        "awaitingEvent",  normalizedEvent,
                        "timeoutAt",      resumeAt.toString()
                    ),
                    "Waiting for email event: " + normalizedEvent
            );
        }
    }
}
