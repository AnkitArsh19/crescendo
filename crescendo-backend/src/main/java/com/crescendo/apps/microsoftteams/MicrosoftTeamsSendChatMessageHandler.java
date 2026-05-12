package com.crescendo.apps.microsoftteams;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends a direct chat message to a user via Microsoft Graph API.
 * Creates a 1:1 chat first, then sends the message.
 */
@ActionMapping(appKey = "microsoft-teams", actionKey = "send-chat-message")
public class MicrosoftTeamsSendChatMessageHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(MicrosoftTeamsSendChatMessageHandler.class);
    private static final String GRAPH_API = "https://graph.microsoft.com/v1.0";
    private final RestClient restClient = RestClient.create();

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = creds != null ? (String) creds.get("accessToken") : null;
        if (accessToken == null || accessToken.isBlank()) {
            return ActionResult.failure("Microsoft Teams requires an OAuth2 accessToken");
        }

        String userId = str(config, "userId");
        String message = str(config, "message");
        if (userId == null) return ActionResult.failure("'userId' is required");
        if (message == null) return ActionResult.failure("'message' is required");

        logger.info("[teams] Sending chat message to user '{}'", userId);

        try {
            // Step 1: Create or get existing 1:1 chat
            Map<String, Object> chatBody = Map.of(
                "chatType", "oneOnOne",
                "members", List.of(
                    Map.of(
                        "@odata.type", "#microsoft.graph.aadUserConversationMember",
                        "roles", List.of("owner"),
                        "user@odata.bind", "https://graph.microsoft.com/v1.0/users('" + userId + "')"
                    )
                )
            );

            Map<String, Object> chat = restClient.post()
                    .uri(GRAPH_API + "/chats")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(chatBody)
                    .retrieve()
                    .body(Map.class);

            String chatId = chat != null ? (String) chat.get("id") : null;
            if (chatId == null) return ActionResult.failure("Failed to create/get chat");

            // Step 2: Send message in the chat
            Map<String, Object> msgBody = Map.of(
                "body", Map.of("content", message)
            );

            String response = restClient.post()
                    .uri(GRAPH_API + "/chats/" + chatId + "/messages")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(msgBody)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "microsoft-teams");
            output.put("action", "send-chat-message");
            output.put("userId", userId);
            output.put("chatId", chatId);
            output.put("response", response);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[teams] Send chat message failed: {}", e.getMessage());
            return ActionResult.failure("Teams send-chat-message failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : null;
    }
}
