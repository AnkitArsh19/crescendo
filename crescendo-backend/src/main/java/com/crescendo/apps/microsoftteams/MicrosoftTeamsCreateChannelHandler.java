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
import java.util.Map;

/**
 * Creates a new channel in a Microsoft Teams team via Graph API.
 */
@ActionMapping(appKey = "microsoft-teams", actionKey = "create-channel")
public class MicrosoftTeamsCreateChannelHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(MicrosoftTeamsCreateChannelHandler.class);
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

        String teamId = str(config, "teamId");
        String channelName = str(config, "channelName");
        if (teamId == null) return ActionResult.failure("'teamId' is required");
        if (channelName == null) return ActionResult.failure("'channelName' is required");

        String description = str(config, "description");

        logger.info("[teams] Creating channel '{}' in team '{}'", channelName, teamId);

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("displayName", channelName);
            body.put("membershipType", "standard");
            if (description != null) body.put("description", description);

            Map<String, Object> response = restClient.post()
                    .uri(GRAPH_API + "/teams/" + teamId + "/channels")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "microsoft-teams");
            output.put("action", "create-channel");
            output.put("teamId", teamId);
            output.put("channelId", response != null ? response.get("id") : null);
            output.put("channelName", channelName);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[teams] Create channel failed: {}", e.getMessage());
            return ActionResult.failure("Teams create-channel failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : null;
    }
}
