package com.crescendo.apps.microsoftoutlook;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Grouped handler for Microsoft Outlook Contact operations.
 */
@Component
public class MicrosoftOutlookContactHandlers {

    private static final String GRAPH_API = MicrosoftOutlookSupport.GRAPH_API;

    // ── create ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "createContact")
// @SuppressWarnings("unchecked")
    public ActionResult create(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String givenName = MicrosoftOutlookSupport.require(config, "givenName");
        if (givenName == null) return ActionResult.failure("'givenName' is required");

        try {
            Map<String, Object> contact = new HashMap<>();
            contact.put("givenName", givenName);
            
            String surname = MicrosoftOutlookSupport.opt(config, "surname", null);
            if (surname != null) contact.put("surname", surname);

            String response = MicrosoftOutlookSupport.clientBuilder(context).build().post()
                    .uri(GRAPH_API + "/me/contacts")
                    .body(contact)
                    .retrieve()
                    .body(String.class);
            
            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            return ActionResult.success(output);
        } catch (Exception e) {
            return ActionResult.failure("Outlook createContact failed: " + e.getMessage());
        }
    }

    // ── delete ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "deleteContact")
    public ActionResult delete(ActionContext context) {
        String contactId = MicrosoftOutlookSupport.require(context.configuration(), "contactId");
        if (contactId == null) return ActionResult.failure("'contactId' is required");

        try {
            MicrosoftOutlookSupport.clientBuilder(context).build().delete()
                    .uri(GRAPH_API + "/me/contacts/" + contactId)
                    .retrieve()
                    .toBodilessEntity();
            return ActionResult.success(Map.of("success", true, "contactId", contactId));
        } catch (Exception e) {
            return ActionResult.failure("Outlook deleteContact failed: " + e.getMessage());
        }
    }

    // ── get ───────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "getContact")
    @SuppressWarnings("unchecked")
    public ActionResult get(ActionContext context) {
        String contactId = MicrosoftOutlookSupport.require(context.configuration(), "contactId");
        if (contactId == null) return ActionResult.failure("'contactId' is required");

        try {
            Map<String, Object> response = MicrosoftOutlookSupport.clientBuilder(context).build().get()
                    .uri(GRAPH_API + "/me/contacts/" + contactId)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Outlook getContact failed: " + e.getMessage());
        }
    }

    // ── getAll ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "getAllContacts")
    @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        Map<String, Object> config = context.configuration();
        int maxResults = MicrosoftOutlookSupport.parseIntOpt(config, "maxResults", 50);

        try {
            Map<String, Object> response = MicrosoftOutlookSupport.clientBuilder(context).build().get()
                    .uri(GRAPH_API + "/me/contacts?$top=" + maxResults)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Outlook getAllContacts failed: " + e.getMessage());
        }
    }

    // ── update ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "updateContact")
    @SuppressWarnings("unchecked")
    public ActionResult update(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String contactId = MicrosoftOutlookSupport.require(config, "contactId");
        if (contactId == null) return ActionResult.failure("'contactId' is required");

        try {
            Map<String, Object> patch = new HashMap<>();
            String givenName = MicrosoftOutlookSupport.opt(config, "givenName", null);
            if (givenName != null) patch.put("givenName", givenName);

            Map<String, Object> response = MicrosoftOutlookSupport.clientBuilder(context).build().patch()
                    .uri(GRAPH_API + "/me/contacts/" + contactId)
                    .body(patch)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Outlook updateContact failed: " + e.getMessage());
        }
    }
}
