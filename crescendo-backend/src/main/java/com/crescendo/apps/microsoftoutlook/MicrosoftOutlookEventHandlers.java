package com.crescendo.apps.microsoftoutlook;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Grouped handler for Microsoft Outlook Event operations.
 */
@Component
public class MicrosoftOutlookEventHandlers {

    private static final String GRAPH_API = MicrosoftOutlookSupport.GRAPH_API;

    // ── create ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "createEvent")
    @SuppressWarnings("unchecked")
    public ActionResult create(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String subject = MicrosoftOutlookSupport.require(config, "subject");
        String start = MicrosoftOutlookSupport.require(config, "start");
        String end = MicrosoftOutlookSupport.require(config, "end");
        if (subject == null || start == null || end == null) {
            return ActionResult.failure("'subject', 'start', and 'end' are required");
        }
        String timeZone = MicrosoftOutlookSupport.opt(config, "timeZone", "UTC");
        String calendarId = MicrosoftOutlookSupport.opt(config, "calendarId", null);

        try {
            Map<String, Object> event = new HashMap<>();
            event.put("subject", subject);
            event.put("start", Map.of("dateTime", start, "timeZone", timeZone));
            event.put("end", Map.of("dateTime", end, "timeZone", timeZone));

            String bodyHtml = MicrosoftOutlookSupport.opt(config, "bodyHtml", null);
            if (bodyHtml != null) event.put("body", Map.of("contentType", "HTML", "content", bodyHtml));

            String uri = GRAPH_API + "/me/" + (calendarId != null ? "calendars/" + calendarId + "/events" : "events");
            Map<String, Object> response = MicrosoftOutlookSupport.clientBuilder(context).build().post()
                    .uri(uri)
                    .body(event)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Outlook createEvent failed: " + e.getMessage());
        }
    }

    // ── delete ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "deleteEvent")
    public ActionResult delete(ActionContext context) {
        String eventId = MicrosoftOutlookSupport.require(context.configuration(), "eventId");
        if (eventId == null) return ActionResult.failure("'eventId' is required");

        try {
            MicrosoftOutlookSupport.clientBuilder(context).build().delete()
                    .uri(GRAPH_API + "/me/events/" + eventId)
                    .retrieve()
                    .toBodilessEntity();
            return ActionResult.success(Map.of("success", true, "eventId", eventId));
        } catch (Exception e) {
            return ActionResult.failure("Outlook deleteEvent failed: " + e.getMessage());
        }
    }

    // ── get ───────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "getEvent")
    @SuppressWarnings("unchecked")
    public ActionResult get(ActionContext context) {
        String eventId = MicrosoftOutlookSupport.require(context.configuration(), "eventId");
        if (eventId == null) return ActionResult.failure("'eventId' is required");

        try {
            Map<String, Object> response = MicrosoftOutlookSupport.clientBuilder(context).build().get()
                    .uri(GRAPH_API + "/me/events/" + eventId)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Outlook getEvent failed: " + e.getMessage());
        }
    }

    // ── getAll ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "getAllEvents")
    @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        Map<String, Object> config = context.configuration();
        int maxResults = MicrosoftOutlookSupport.parseIntOpt(config, "maxResults", 50);
        String calendarId = MicrosoftOutlookSupport.opt(config, "calendarId", null);

        try {
            String uri = GRAPH_API + "/me/" + (calendarId != null ? "calendars/" + calendarId + "/events" : "events") + "?$top=" + maxResults;
            Map<String, Object> response = MicrosoftOutlookSupport.clientBuilder(context).build().get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Outlook getAllEvents failed: " + e.getMessage());
        }
    }

    // ── update ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftoutlook", actionKey = "updateEvent")
    @SuppressWarnings("unchecked")
    public ActionResult update(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String eventId = MicrosoftOutlookSupport.require(config, "eventId");
        if (eventId == null) return ActionResult.failure("'eventId' is required");

        try {
            Map<String, Object> patch = new HashMap<>();
            String subject = MicrosoftOutlookSupport.opt(config, "subject", null);
            if (subject != null) patch.put("subject", subject);

            Map<String, Object> response = MicrosoftOutlookSupport.clientBuilder(context).build().patch()
                    .uri(GRAPH_API + "/me/events/" + eventId)
                    .body(patch)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Outlook updateEvent failed: " + e.getMessage());
        }
    }
}
