package com.crescendo.apps.microsoftoutlook;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

// import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Grouped handler for Microsoft Outlook Attachment operations.
 */
@Component
public class MicrosoftOutlookAttachmentHandlers {

    private static final String GRAPH_API = MicrosoftOutlookSupport.GRAPH_API;

    // ── add ───────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "addAttachment")
    @SuppressWarnings("unchecked")
    public ActionResult add(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String messageId = MicrosoftOutlookSupport.require(config, "messageId");
        String name = MicrosoftOutlookSupport.require(config, "name");
        String contentBytes = MicrosoftOutlookSupport.require(config, "contentBytes"); // Base64
        
        if (messageId == null || name == null || contentBytes == null) {
            return ActionResult.failure("'messageId', 'name', and 'contentBytes' are required");
        }

        try {
            Map<String, Object> attachment = new HashMap<>();
            attachment.put("@odata.type", "#microsoft.graph.fileAttachment");
            attachment.put("name", name);
            attachment.put("contentBytes", contentBytes);
            
            String contentType = MicrosoftOutlookSupport.opt(config, "contentType", null);
            if (contentType != null) attachment.put("contentType", contentType);

            Map<String, Object> response = MicrosoftOutlookSupport.clientBuilder(context).build().post()
                    .uri(GRAPH_API + "/me/messages/" + messageId + "/attachments")
                    .body(attachment)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Outlook addAttachment failed: " + e.getMessage());
        }
    }

    // ── download ──────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "downloadAttachment")
    @SuppressWarnings("unchecked")
    public ActionResult download(ActionContext context) {
        String messageId = MicrosoftOutlookSupport.require(context.configuration(), "messageId");
        String attachmentId = MicrosoftOutlookSupport.require(context.configuration(), "attachmentId");
        if (messageId == null || attachmentId == null) {
            return ActionResult.failure("'messageId' and 'attachmentId' are required");
        }

        try {
            Map<String, Object> response = MicrosoftOutlookSupport.clientBuilder(context).build().get()
                    .uri(GRAPH_API + "/me/messages/" + messageId + "/attachments/" + attachmentId)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Outlook downloadAttachment failed: " + e.getMessage());
        }
    }

    // ── get ───────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "getAttachment")
    @SuppressWarnings("unchecked")
    public ActionResult get(ActionContext context) {
        String messageId = MicrosoftOutlookSupport.require(context.configuration(), "messageId");
        String attachmentId = MicrosoftOutlookSupport.require(context.configuration(), "attachmentId");
        if (messageId == null || attachmentId == null) {
            return ActionResult.failure("'messageId' and 'attachmentId' are required");
        }

        try {
            Map<String, Object> response = MicrosoftOutlookSupport.clientBuilder(context).build().get()
                    .uri(GRAPH_API + "/me/messages/" + messageId + "/attachments/" + attachmentId)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Outlook getAttachment failed: " + e.getMessage());
        }
    }

    // ── getAll ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "getAllAttachments")
    @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        String messageId = MicrosoftOutlookSupport.require(context.configuration(), "messageId");
        if (messageId == null) return ActionResult.failure("'messageId' is required");

        try {
            Map<String, Object> response = MicrosoftOutlookSupport.clientBuilder(context).build().get()
                    .uri(GRAPH_API + "/me/messages/" + messageId + "/attachments")
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Outlook getAllAttachments failed: " + e.getMessage());
        }
    }
}
