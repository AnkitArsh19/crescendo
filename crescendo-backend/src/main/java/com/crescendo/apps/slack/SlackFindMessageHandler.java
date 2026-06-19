package com.crescendo.apps.slack;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Finds a message in Slack via search.messages.
 */
@ActionMapping(appKey = "slack", actionKey = "find-message")
@SuppressWarnings("unchecked")
public class SlackFindMessageHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(SlackFindMessageHandler.class);
    private final RestClient restClient = RestClient.create();

    @Override
public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();
        String token = resolveToken(creds);
        if (token == null) return ActionResult.failure("Slack requires a bot token");

        String query = str(config, "query");
        if (query == null) return ActionResult.failure("'query' is required");

        String count = str(config, "maxResults");
        if (count == null) count = "5";

        logger.info("[slack] Searching messages: query='{}'", query);

        try {
            String url = "https://slack.com/api/search.messages?query="
                    + java.net.URLEncoder.encode(query, "UTF-8")
                    + "&count=" + count;

            Map<String, Object> response = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "slack");
            output.put("action", "find-message");
            output.put("query", query);

            if (response != null && response.containsKey("messages")) {
                var messages = (Map<String, Object>) response.get("messages");
                output.put("total", messages.get("total"));
                output.put("matches", messages.get("matches"));
            }
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[slack] Find message failed: {}", e.getMessage());
            return ActionResult.failure("Slack find-message failed: " + e.getMessage());
        }
    }

    private String resolveToken(Map<String, Object> creds) {
        if (creds == null) return null;
        String t = (String) creds.get("botToken");
        if (t == null || t.isBlank()) t = (String) creds.get("accessToken");
        return (t != null && !t.isBlank()) ? t : null;
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : null;
    }
}
