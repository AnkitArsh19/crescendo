package com.crescendo.apps.microsoftteams;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Grouped handler for Microsoft Teams Chat operations.
 */
@Component
public class MicrosoftTeamsChatHandlers {

    private static final String GRAPH_API = MicrosoftTeamsSupport.GRAPH_API;

    // ── send ──────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftteams", actionKey = "sendChatMessage")
    @SuppressWarnings("unchecked")
    public ActionResult send(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String chatId = MicrosoftTeamsSupport.require(config, "chatId");
        String message = MicrosoftTeamsSupport.require(config, "message");

        if (chatId == null || message == null) {
            return ActionResult.failure("'chatId' and 'message' are required");
        }

        try {
            String contentType = MicrosoftTeamsSupport.opt(config, "contentType", "html");
            
            Map<String, Object> body = Map.of(
                    "body", Map.of(
                            "contentType", contentType,
                            "content", message
                    )
            );

            Map<String, Object> response = MicrosoftTeamsSupport.clientBuilder(context).build().post()
                    .uri(GRAPH_API + "/chats/" + chatId + "/messages")
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Teams sendChatMessage failed: " + e.getMessage());
        }
    }

    // ── get ───────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftteams", actionKey = "getChat")
    @SuppressWarnings("unchecked")
    public ActionResult get(ActionContext context) {
        String chatId = MicrosoftTeamsSupport.require(context.configuration(), "chatId");

        if (chatId == null) {
            return ActionResult.failure("'chatId' is required");
        }

        try {
            Map<String, Object> response = MicrosoftTeamsSupport.clientBuilder(context).build().get()
                    .uri(GRAPH_API + "/chats/" + chatId)
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Teams getChat failed: " + e.getMessage());
        }
    }

    // ── getAll ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftteams", actionKey = "getAllChats")
    @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        try {
            Map<String, Object> response = MicrosoftTeamsSupport.clientBuilder(context).build().get()
                    .uri(GRAPH_API + "/me/chats")
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Teams getAllChats failed: " + e.getMessage());
        }
    }
}
