package com.crescendo.apps.crescendomail;

import com.crescendo.emailservice.suppression.EmailSuppressionService;
import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

import static com.crescendo.apps.crescendomail.CrescendoMailSendHandlers.blank;
import static com.crescendo.apps.crescendomail.CrescendoMailSendHandlers.resolveUserId;

/**
 * Suppression action handlers for crescendomail.
 *
 * <p>Action keys:
 * <ul>
 *   <li>{@code crescendomail:suppress}   — manually suppress a recipient from all sends
 *   <li>{@code crescendomail:unsuppress} — lift a suppression after a support review
 * </ul>
 */
public class CrescendoMailSuppressionHandlers {

    // ─────────────────────────────────────────────────────────────────────────
    // suppress
    // ─────────────────────────────────────────────────────────────────────────

    @Component
    @ActionMapping(appKey = "crescendomail", actionKey = "suppress")
    public static class SuppressHandler implements ActionHandler {

        private static final Logger log = LoggerFactory.getLogger(SuppressHandler.class);

        private final EmailSuppressionService suppressionService;

        public SuppressHandler(EmailSuppressionService suppressionService) {
            this.suppressionService = suppressionService;
        }

        @Override
        public ActionResult execute(ActionContext ctx) {
            String email  = ctx.getString("email");
            String reason = ctx.getString("reason");

            if (blank(email)) return ActionResult.failure("crescendomail:suppress requires 'email'");

            UUID userId = resolveUserId(ctx);
            suppressionService.suppress(userId, email, reason != null ? reason : "manual");

            log.info("[crescendomail:suppress] Suppressed {} for user {}", email, userId);
            return ActionResult.success(Map.of("email", email, "suppressed", true));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // unsuppress
    // ─────────────────────────────────────────────────────────────────────────

    @Component
    @ActionMapping(appKey = "crescendomail", actionKey = "unsuppress")
    public static class UnsuppressHandler implements ActionHandler {

        private static final Logger log = LoggerFactory.getLogger(UnsuppressHandler.class);

        private final EmailSuppressionService suppressionService;

        public UnsuppressHandler(EmailSuppressionService suppressionService) {
            this.suppressionService = suppressionService;
        }

        @Override
        public ActionResult execute(ActionContext ctx) {
            String suppressionIdStr = ctx.getString("suppressionId");
            if (blank(suppressionIdStr)) return ActionResult.failure("crescendomail:unsuppress requires 'suppressionId'");

            UUID suppressionId;
            try {
                suppressionId = UUID.fromString(suppressionIdStr);
            } catch (IllegalArgumentException e) {
                return ActionResult.failure("Invalid suppressionId format");
            }

            UUID userId = resolveUserId(ctx);
            suppressionService.remove(userId, suppressionId);

            log.info("[crescendomail:unsuppress] Removed suppression {} for user {}", suppressionId, userId);
            return ActionResult.success(Map.of("suppressionId", suppressionIdStr, "removed", true));
        }
    }
}
