package com.crescendo.apps.microsoftoutlook;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Grouped handler for Microsoft Outlook Draft operations.
 */
@Component
public class MicrosoftOutlookDraftHandlers {

    private static final String GRAPH_API = MicrosoftOutlookSupport.GRAPH_API;

    // ── create ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "createDraft")
    @SuppressWarnings("unchecked")
    public ActionResult create(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String subject = MicrosoftOutlookSupport.opt(config, "subject", "No Subject");
        String bodyHtml = MicrosoftOutlookSupport.opt(config, "bodyHtml", "");

        try {
            Map<String, Object> message = new HashMap<>();
            message.put("subject", subject);
            message.put("body", Map.of("contentType", "HTML", "content", bodyHtml));

            Map<String, Object> response = MicrosoftOutlookSupport.clientBuilder(context).build().post()
                    .uri(GRAPH_API + "/me/messages")
                    .body(message)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Outlook createDraft failed: " + e.getMessage());
        }
    }

    // ── delete ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "deleteDraft")
    public ActionResult delete(ActionContext context) {
        String draftId = MicrosoftOutlookSupport.require(context.configuration(), "draftId");
        if (draftId == null) return ActionResult.failure("'draftId' is required");

        try {
            MicrosoftOutlookSupport.clientBuilder(context).build().delete()
                    .uri(GRAPH_API + "/me/messages/" + draftId)
                    .retrieve()
                    .toBodilessEntity();
            return ActionResult.success(Map.of("success", true, "draftId", draftId));
        } catch (Exception e) {
            return ActionResult.failure("Outlook deleteDraft failed: " + e.getMessage());
        }
    }

    // ── get ───────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "getDraft")
    @SuppressWarnings("unchecked")
    public ActionResult get(ActionContext context) {
        String draftId = MicrosoftOutlookSupport.require(context.configuration(), "draftId");
        if (draftId == null) return ActionResult.failure("'draftId' is required");

        try {
            Map<String, Object> response = MicrosoftOutlookSupport.clientBuilder(context).build().get()
                    .uri(GRAPH_API + "/me/messages/" + draftId)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Outlook getDraft failed: " + e.getMessage());
        }
    }

    // ── send ──────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "sendDraft")
    public ActionResult send(ActionContext context) {
        String draftId = MicrosoftOutlookSupport.require(context.configuration(), "draftId");
        if (draftId == null) return ActionResult.failure("'draftId' is required");

        try {
            MicrosoftOutlookSupport.clientBuilder(context).build().post()
                    .uri(GRAPH_API + "/me/messages/" + draftId + "/send")
                    .retrieve()
                    .toBodilessEntity();
            return ActionResult.success(Map.of("success", true, "draftId", draftId));
        } catch (Exception e) {
            return ActionResult.failure("Outlook sendDraft failed: " + e.getMessage());
        }
    }

    // ── update ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "updateDraft")
    @SuppressWarnings("unchecked")
    public ActionResult update(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String draftId = MicrosoftOutlookSupport.require(config, "draftId");
        if (draftId == null) return ActionResult.failure("'draftId' is required");

        try {
            Map<String, Object> patch = new HashMap<>();
            String subject = MicrosoftOutlookSupport.opt(config, "subject", null);
            if (subject != null) patch.put("subject", subject);

            String bodyHtml = MicrosoftOutlookSupport.opt(config, "bodyHtml", null);
            if (bodyHtml != null) patch.put("body", Map.of("contentType", "HTML", "content", bodyHtml));

            Map<String, Object> response = MicrosoftOutlookSupport.clientBuilder(context).build().patch()
                    .uri(GRAPH_API + "/me/messages/" + draftId)
                    .body(patch)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Outlook updateDraft failed: " + e.getMessage());
        }
    }
}
