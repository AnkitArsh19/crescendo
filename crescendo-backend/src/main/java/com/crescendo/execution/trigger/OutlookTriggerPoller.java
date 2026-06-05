package com.crescendo.execution.trigger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Polling trigger for Microsoft Outlook — checks for new emails using the
 * Microsoft Graph API.
 *
 * Supports the trigger key "new-email" for the "microsoft-outlook" app.
 * Uses the /me/messages endpoint with $filter to find emails
 * received since the last poll.
 */
@Component
public class OutlookTriggerPoller implements TriggerPoller {

    private static final Logger logger = LoggerFactory.getLogger(OutlookTriggerPoller.class);
    private static final String GRAPH_MESSAGES_URL = "https://graph.microsoft.com/v1.0/me/messages";

    private final RestClient restClient;

    public OutlookTriggerPoller() {
        this.restClient = RestClient.create();
    }

    @Override
    public boolean supports(String appKey, String triggerKey) {
        return "microsoft-outlook".equals(appKey) && "new-email".equals(triggerKey);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> poll(Map<String, Object> credentials,
                                          Map<String, Object> configuration,
                                          Instant lastPollTime) {
        String accessToken = String.valueOf(credentials.get("accessToken"));

        String isoTime = DateTimeFormatter.ISO_INSTANT.format(lastPollTime.atZone(ZoneOffset.UTC));
        String filter = "receivedDateTime ge " + isoTime;

        // Apply user-configured filters
        if (configuration != null) {
            logger.debug("[outlook-poller] Raw configuration: {}", configuration);

            String subjectFilter = asStr(configuration.get("subjectFilter"));
            if (subjectFilter != null && !subjectFilter.isBlank()) {
                filter += " and contains(subject, '" + subjectFilter.replace("'", "''") + "')";
            }
            String fromFilter = asStr(configuration.get("fromFilter"));
            if (fromFilter != null && !fromFilter.isBlank()) {
                filter += " and from/emailAddress/address eq '" + fromFilter.replace("'", "''") + "'";
            }
            // Legacy fallback key
            String senderEmail = asStr(configuration.get("senderEmail"));
            if (senderEmail != null && !senderEmail.isBlank() &&
                    (fromFilter == null || fromFilter.isBlank())) {
                filter += " and from/emailAddress/address eq '" + senderEmail.replace("'", "''") + "'";
            }
        }

        // Build URL with folder if specified
        String baseUrl = GRAPH_MESSAGES_URL;
        if (configuration != null) {
            String folderId = asStr(configuration.get("folderId"));
            if (folderId != null && !folderId.isBlank()) {
                baseUrl = "https://graph.microsoft.com/v1.0/me/mailFolders/" + folderId + "/messages";
            }
        }

        // URL-encode the filter to avoid OData syntax issues, but use URI.create()
        // so Spring does NOT double-encode the already-encoded query string.
        String encodedFilter = URLEncoder.encode(filter, StandardCharsets.UTF_8)
                .replace("+", "%20"); // OData requires %20 not +
        String url = baseUrl
                + "?$filter=" + encodedFilter
                + "&$top=10"
                + "&$orderby=receivedDateTime%20desc"
                + "&$select=id,subject,from,receivedDateTime,bodyPreview";

        logger.info("[outlook-poller] Polling Outlook: url='{}', since='{}'", url, isoTime);

        try {
            // Use URI.create() to prevent Spring RestClient from re-encoding
            // the OData query parameters ($ signs, spaces, etc.)
            Map<String, Object> response = restClient.get()
                    .uri(URI.create(url))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            logger.debug("[outlook-poller] Raw API response: {}", response);

            if (response == null || !response.containsKey("value")) {
                logger.info("[outlook-poller] No new messages found");
                return List.of();
            }

            List<Map<String, Object>> messages = (List<Map<String, Object>>) response.get("value");
            if (messages == null || messages.isEmpty()) {
                logger.info("[outlook-poller] Empty value array in response");
                return List.of();
            }

            logger.info("[outlook-poller] Found {} new message(s)", messages.size());

            List<Map<String, Object>> results = new ArrayList<>();
            for (Map<String, Object> msg : messages) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("id", msg.get("id"));
                payload.put("subject", msg.get("subject"));
                payload.put("bodyPreview", msg.get("bodyPreview"));
                payload.put("receivedDateTime", msg.get("receivedDateTime"));

                Map<String, Object> from = (Map<String, Object>) msg.get("from");
                if (from != null) {
                    Map<String, Object> emailAddr = (Map<String, Object>) from.get("emailAddress");
                    if (emailAddr != null) {
                        payload.put("fromEmail", emailAddr.get("address"));
                        payload.put("fromName", emailAddr.get("name"));
                    }
                }

                logger.debug("[outlook-poller] Email: subject='{}', from='{}'",
                        payload.get("subject"), payload.get("fromEmail"));
                results.add(payload);
            }

            return results;
        } catch (Exception e) {
            logger.error("[outlook-poller] Failed to poll: {} — {}", e.getClass().getSimpleName(), e.getMessage());
            return List.of();
        }
    }

    private static String asStr(Object obj) {
        return obj != null ? String.valueOf(obj) : null;
    }
}
