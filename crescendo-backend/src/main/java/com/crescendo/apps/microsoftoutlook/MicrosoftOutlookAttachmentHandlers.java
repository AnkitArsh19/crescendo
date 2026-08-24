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
    private final com.crescendo.storage.MediaStreamResolver mediaStreamResolver;

    public MicrosoftOutlookAttachmentHandlers() {
        this(new com.crescendo.storage.MediaStreamResolver());
    }

    public MicrosoftOutlookAttachmentHandlers(@org.springframework.beans.factory.annotation.Autowired com.crescendo.storage.MediaStreamResolver mediaStreamResolver) {
        this.mediaStreamResolver = mediaStreamResolver;
    }

    // ── add ───────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoft-outlook", actionKey = "addAttachment")
    @SuppressWarnings("unchecked")
    public ActionResult add(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String messageId = MicrosoftOutlookSupport.require(config, "messageId");
        String name = MicrosoftOutlookSupport.opt(config, "name", null);
        Object rawContent = config.get("contentBytes");
        if (rawContent == null) rawContent = config.get("file");
        if (rawContent == null) rawContent = config.get("attachment");

        if (messageId == null || rawContent == null) {
            return ActionResult.failure("'messageId' and 'file' (or 'contentBytes') are required");
        }

        try {
            String base64Bytes;
            String resolvedContentType = MicrosoftOutlookSupport.opt(config, "contentType", null);
            String resolvedName = name;

            if (mediaStreamResolver != null) {
                try (com.crescendo.storage.MediaStreamResolver.MediaSource media = mediaStreamResolver.resolve(rawContent, resolvedContentType != null ? resolvedContentType : "application/octet-stream")) {
                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                    media.stream().transferTo(baos);
                    base64Bytes = java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
                    if (resolvedName == null || resolvedName.isBlank()) {
                        resolvedName = media.filename() != null ? media.filename() : "attachment.dat";
                    }
                    if (resolvedContentType == null || resolvedContentType.isBlank()) {
                        resolvedContentType = media.contentType();
                    }
                }
            } else {
                base64Bytes = rawContent.toString();
                if (resolvedName == null) resolvedName = "attachment.dat";
            }

            Map<String, Object> attachment = new HashMap<>();
            attachment.put("@odata.type", "#microsoft.graph.fileAttachment");
            attachment.put("name", resolvedName);
            attachment.put("contentBytes", base64Bytes);

            if (resolvedContentType != null) attachment.put("contentType", resolvedContentType);

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
    @ActionMapping(appKey = "microsoft-outlook", actionKey = "downloadAttachment")
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
    @ActionMapping(appKey = "microsoft-outlook", actionKey = "getAttachment")
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
    @ActionMapping(appKey = "microsoft-outlook", actionKey = "getAllAttachments")
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
