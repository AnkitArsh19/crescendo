package com.crescendo.apps.microsoftoutlook;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Grouped handler for Microsoft Outlook Folder operations.
 */
@Component
public class MicrosoftOutlookFolderHandlers {

    private static final String GRAPH_API = MicrosoftOutlookSupport.GRAPH_API;

    // ── create ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "createFolder")
    @SuppressWarnings("unchecked")
    public ActionResult create(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String displayName = MicrosoftOutlookSupport.require(config, "displayName");
        if (displayName == null) return ActionResult.failure("'displayName' is required");

        try {
            Map<String, Object> response = MicrosoftOutlookSupport.clientBuilder(context).build().post()
                    .uri(GRAPH_API + "/me/mailFolders")
                    .body(Map.of("displayName", displayName))
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Outlook createFolder failed: " + e.getMessage());
        }
    }

    // ── delete ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "deleteFolder")
    public ActionResult delete(ActionContext context) {
        String folderId = MicrosoftOutlookSupport.require(context.configuration(), "folderId");
        if (folderId == null) return ActionResult.failure("'folderId' is required");

        try {
            MicrosoftOutlookSupport.clientBuilder(context).build().delete()
                    .uri(GRAPH_API + "/me/mailFolders/" + folderId)
                    .retrieve()
                    .toBodilessEntity();
            return ActionResult.success(Map.of("success", true, "folderId", folderId));
        } catch (Exception e) {
            return ActionResult.failure("Outlook deleteFolder failed: " + e.getMessage());
        }
    }

    // ── get ───────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "getFolder")
    @SuppressWarnings("unchecked")
    public ActionResult get(ActionContext context) {
        String folderId = MicrosoftOutlookSupport.require(context.configuration(), "folderId");
        if (folderId == null) return ActionResult.failure("'folderId' is required");

        try {
            Map<String, Object> response = MicrosoftOutlookSupport.clientBuilder(context).build().get()
                    .uri(GRAPH_API + "/me/mailFolders/" + folderId)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Outlook getFolder failed: " + e.getMessage());
        }
    }

    // ── getAll ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "getAllFolders")
    @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        int maxResults = MicrosoftOutlookSupport.parseIntOpt(context.configuration(), "maxResults", 50);

        try {
            Map<String, Object> response = MicrosoftOutlookSupport.clientBuilder(context).build().get()
                    .uri(GRAPH_API + "/me/mailFolders?$top=" + maxResults)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Outlook getAllFolders failed: " + e.getMessage());
        }
    }

    // ── update ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "updateFolder")
    @SuppressWarnings("unchecked")
    public ActionResult update(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String folderId = MicrosoftOutlookSupport.require(config, "folderId");
        if (folderId == null) return ActionResult.failure("'folderId' is required");

        try {
            Map<String, Object> patch = new HashMap<>();
            String displayName = MicrosoftOutlookSupport.opt(config, "displayName", null);
            if (displayName != null) patch.put("displayName", displayName);

            Map<String, Object> response = MicrosoftOutlookSupport.clientBuilder(context).build().patch()
                    .uri(GRAPH_API + "/me/mailFolders/" + folderId)
                    .body(patch)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Outlook updateFolder failed: " + e.getMessage());
        }
    }
}
