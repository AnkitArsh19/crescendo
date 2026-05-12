package com.crescendo.apps.gmail;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Searches Gmail messages using the Gmail advanced search query syntax.
 */
@ActionMapping(appKey = "gmail", actionKey = "search-emails")
public class GmailSearchHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GmailSearchHandler.class);
    private static final String GMAIL_LIST_API = "https://gmail.googleapis.com/gmail/v1/users/me/messages";
    private static final String GMAIL_MSG_API = "https://gmail.googleapis.com/gmail/v1/users/me/messages/";

    private final RestClient restClient;

    public GmailSearchHandler() {
        this.restClient = RestClient.create();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = creds != null ? (String) creds.get("accessToken") : null;
        if (accessToken == null || accessToken.isBlank()) {
            return ActionResult.failure("Gmail requires an OAuth2 accessToken");
        }

        String query = str(config, "query");
        if (query == null) return ActionResult.failure("'query' is required");

        int maxResults = 10;
        String maxStr = str(config, "maxResults");
        if (maxStr != null) {
            try { maxResults = Integer.parseInt(maxStr); } catch (NumberFormatException ignored) {}
        }

        logger.info("[gmail] Searching emails: query='{}', max={}", query, maxResults);

        try {
            String url = GMAIL_LIST_API + "?q=" + java.net.URLEncoder.encode(query, "UTF-8")
                    + "&maxResults=" + maxResults;

            Map<String, Object> listResponse = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            List<Map<String, Object>> messages = (List<Map<String, Object>>) listResponse.get("messages");
            if (messages == null) messages = List.of();

            // Fetch metadata for each message
            List<Map<String, Object>> results = new ArrayList<>();
            for (Map<String, Object> msg : messages) {
                String msgId = (String) msg.get("id");
                try {
                    Map<String, Object> detail = restClient.get()
                            .uri(GMAIL_MSG_API + msgId + "?format=metadata&metadataHeaders=Subject&metadataHeaders=From&metadataHeaders=Date")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .retrieve()
                            .body(Map.class);

                    Map<String, Object> entry = new HashMap<>();
                    entry.put("id", msgId);
                    entry.put("threadId", detail.get("threadId"));
                    entry.put("snippet", detail.get("snippet"));

                    var payload = (Map<String, Object>) detail.get("payload");
                    if (payload != null) {
                        var headers = (List<Map<String, String>>) payload.get("headers");
                        if (headers != null) {
                            for (var h : headers) {
                                String name = h.get("name");
                                if ("Subject".equalsIgnoreCase(name)) entry.put("subject", h.get("value"));
                                if ("From".equalsIgnoreCase(name)) entry.put("from", h.get("value"));
                                if ("Date".equalsIgnoreCase(name)) entry.put("date", h.get("value"));
                            }
                        }
                    }
                    results.add(entry);
                } catch (Exception ignored) {
                    results.add(Map.of("id", msgId, "error", "failed to fetch details"));
                }
            }

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "gmail");
            output.put("action", "search-emails");
            output.put("query", query);
            output.put("resultCount", results.size());
            output.put("messages", results);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[gmail] Search failed: {}", e.getMessage());
            return ActionResult.failure("Gmail search failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : null;
    }
}
