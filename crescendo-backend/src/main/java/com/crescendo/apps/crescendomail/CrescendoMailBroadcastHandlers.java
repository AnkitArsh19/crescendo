package com.crescendo.apps.crescendomail;

import com.crescendo.emailservice.broadcast.Broadcast;
import com.crescendo.emailservice.broadcast.BroadcastDto;
import com.crescendo.emailservice.broadcast.BroadcastService;
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
 * Broadcast action handlers for crescendomail.
 *
 * <p>Action keys:
 * <ul>
 *   <li>{@code crescendomail:create-broadcast} — create a draft broadcast targeting an audience
 *   <li>{@code crescendomail:send-broadcast}   — send or schedule an existing draft broadcast
 * </ul>
 */
public class CrescendoMailBroadcastHandlers {

    // ─────────────────────────────────────────────────────────────────────────
    // create-broadcast
    // ─────────────────────────────────────────────────────────────────────────

    @Component
    @ActionMapping(appKey = "crescendomail", actionKey = "create-broadcast")
    public static class CreateBroadcastHandler implements ActionHandler {

        private static final Logger log = LoggerFactory.getLogger(CreateBroadcastHandler.class);

        private final BroadcastService broadcastService;

        public CreateBroadcastHandler(BroadcastService broadcastService) {
            this.broadcastService = broadcastService;
        }

        @Override
        public ActionResult execute(ActionContext ctx) {
            String fromAddress = ctx.getString("fromAddress");
            String templateIdStr = ctx.getString("templateId");

            if (blank(fromAddress))   return ActionResult.failure("crescendomail:create-broadcast requires 'fromAddress'");
            if (blank(templateIdStr)) return ActionResult.failure("crescendomail:create-broadcast requires 'templateId'");

            UUID templateId;
            try {
                templateId = UUID.fromString(templateIdStr);
            } catch (IllegalArgumentException e) {
                return ActionResult.failure("Invalid templateId format");
            }

            UUID userId = resolveUserId(ctx);
            Broadcast broadcast = broadcastService.create(userId,
                    new BroadcastDto.CreateBroadcastRequest(templateId, fromAddress));

            log.info("[crescendomail:create-broadcast] Created broadcast {} for user {}", broadcast.getId(), userId);
            return ActionResult.success(Map.of(
                    "broadcastId",  broadcast.getId().toString(),
                    "fromAddress",  broadcast.getFromAddress(),
                    "templateId",   broadcast.getTemplateId().toString(),
                    "status",       broadcast.getStatus().name()
            ));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // send-broadcast
    // ─────────────────────────────────────────────────────────────────────────

    @Component
    @ActionMapping(appKey = "crescendomail", actionKey = "send-broadcast")
    public static class SendBroadcastHandler implements ActionHandler {

        private static final Logger log = LoggerFactory.getLogger(SendBroadcastHandler.class);

        private final BroadcastService broadcastService;

        public SendBroadcastHandler(BroadcastService broadcastService) {
            this.broadcastService = broadcastService;
        }

        @Override
        public ActionResult execute(ActionContext ctx) {
            String broadcastIdStr = ctx.getString("broadcastId");
            if (blank(broadcastIdStr)) return ActionResult.failure("crescendomail:send-broadcast requires 'broadcastId'");

            UUID broadcastId;
            try {
                broadcastId = UUID.fromString(broadcastIdStr);
            } catch (IllegalArgumentException e) {
                return ActionResult.failure("Invalid broadcastId format");
            }

            UUID userId = resolveUserId(ctx);
            Broadcast broadcast = broadcastService.send(userId, broadcastId);

            log.info("[crescendomail:send-broadcast] Sent broadcast {} for user {}", broadcastId, userId);
            return ActionResult.success(Map.of(
                    "broadcastId", broadcast.getId().toString(),
                    "status",      broadcast.getStatus().name()
            ));
        }
    }
}
