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
import java.util.*;

/**
 * Polling trigger for Gmail — checks for new emails using the Gmail API.
 *
 * Supports the trigger key "new-email" for the "gmail" app.
 * Uses the Gmail messages.list endpoint with a time-based query
 * and optional labelIds to find emails received since the last poll.
 */
@Component
public class GmailTriggerPoller implements TriggerPoller {

    private static final Logger logger = LoggerFactory.getLogger(GmailTriggerPoller.class);
    private static final String GMAIL_MESSAGES_URL = "https://gmail.googleapis.com/gmail/v1/users/me/messages";

    private final RestClient restClient;

    public GmailTriggerPoller() {
        this.restClient = RestClient.create();
    }

    @Override
    public boolean supports(String appKey, String triggerKey) {
        return "gmail".equals(appKey) && "new-email".equals(triggerKey);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> poll(Map<String, Object> credentials,
                                          Map<String, Object> configuration,
                                          Instant lastPollTime) {
        String accessToken = String.valueOf(credentials.get("accessToken"));

        // Build Gmail search query
        StringBuilder query = new StringBuilder();
        long epochSeconds = lastPollTime.getEpochSecond();
        query.append("after:").append(epochSeconds);

        // Collect labelIds separately (Gmail API uses a distinct query parameter)
        String labelIds = null;

        // Apply user-configured filters
        if (configuration != null) {
            logger.debug("[gmail-poller] Raw configuration: {}", configuration);

            String subjectFilter = asStr(configuration.get("subjectFilter"));
            if (subjectFilter != null && !subjectFilter.isBlank()) {
                // Quote the subject for exact phrase matching
                query.append(" subject:\"").append(subjectFilter.replace("\"", "")).append("\"");
            }

            String fromFilter = asStr(configuration.get("fromFilter"));
            if (fromFilter != null && !fromFilter.isBlank()) {
                query.append(" from:").append(fromFilter);
            }

            // Also support "senderEmail" as legacy config key
            String senderEmail = asStr(configuration.get("senderEmail"));
            if (senderEmail != null && !senderEmail.isBlank() &&
                    (fromFilter == null || fromFilter.isBlank())) {
                query.append(" from:").append(senderEmail);
            }

            // labelFilter (e.g., "INBOX") -> passed as labelIds query param
            String labelFilter = asStr(configuration.get("labelFilter"));
            if (labelFilter != null && !labelFilter.isBlank()) {
                labelIds = labelFilter;
            }
        }

        // Build full URL with labelIds if present
        // URL-encode the query to handle special characters in subject/from filters
        String encodedQuery = URLEncoder.encode(query.toString(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        String url = GMAIL_MESSAGES_URL + "?q=" + encodedQuery + "&maxResults=10";
        if (labelIds != null) {
            url += "&labelIds=" + labelIds;
        }

        logger.info("[gmail-poller] Polling Gmail: url='{}', after epoch={} ({})",
                url, epochSeconds, Instant.ofEpochSecond(epochSeconds));

        try {
            // Use URI.create() to prevent Spring RestClient from re-encoding
            Map<String, Object> response = restClient.get()
                    .uri(URI.create(url))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            logger.debug("[gmail-poller] Raw API response: {}", response);

            if (response == null || !response.containsKey("messages")) {
                int resultSize = response != null && response.containsKey("resultSizeEstimate")
                        ? ((Number) response.get("resultSizeEstimate")).intValue() : 0;
                logger.info("[gmail-poller] No new messages found (resultSizeEstimate={})", resultSize);
                return List.of();
            }

            List<Map<String, Object>> messages = (List<Map<String, Object>>) response.get("messages");
            if (messages == null || messages.isEmpty()) {
                return List.of();
            }

            logger.info("[gmail-poller] Found {} new message(s)", messages.size());

            // Fetch basic details for each message
            List<Map<String, Object>> results = new ArrayList<>();
            for (Map<String, Object> msg : messages) {
                String messageId = String.valueOf(msg.get("id"));
                try {
                    Map<String, Object> detail = restClient.get()
                            .uri("https://gmail.googleapis.com/gmail/v1/users/me/messages/{id}?format=metadata&metadataHeaders=Subject&metadataHeaders=From&metadataHeaders=Date",
                                    messageId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .retrieve()
                            .body(Map.class);

                    if (detail != null) {
                        Map<String, Object> triggerPayload = extractEmailPayload(detail);
                        triggerPayload.put("messageId", messageId);
                        logger.debug("[gmail-poller] Email detail: subject='{}', from='{}'",
                                triggerPayload.get("subject"), triggerPayload.get("from"));
                        results.add(triggerPayload);
                    }
                } catch (Exception e) {
                    logger.warn("[gmail-poller] Failed to fetch message {}: {}", messageId, e.getMessage());
                }
            }

            return results;
        } catch (Exception e) {
            logger.error("[gmail-poller] Failed to poll Gmail: {} — {}", e.getClass().getSimpleName(), e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractEmailPayload(Map<String, Object> messageDetail) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", messageDetail.get("id"));
        payload.put("threadId", messageDetail.get("threadId"));
        payload.put("snippet", messageDetail.get("snippet"));

        Map<String, Object> payloadSection = (Map<String, Object>) messageDetail.get("payload");
        if (payloadSection != null) {
            List<Map<String, String>> headers = (List<Map<String, String>>) payloadSection.get("headers");
            if (headers != null) {
                for (Map<String, String> header : headers) {
                    String name = header.get("name");
                    if ("Subject".equalsIgnoreCase(name)) {
                        payload.put("subject", header.get("value"));
                    } else if ("From".equalsIgnoreCase(name)) {
                        String raw = header.get("value");
                        // Keep raw "from" for backward compat
                        payload.put("from", raw);
                        // Also emit normalized fields matching frontend variable hints:
                        // e.g. "John Doe <john@example.com>" or just "john@example.com"
                        if (raw != null && raw.contains("<")) {
                            int lt = raw.indexOf('<');
                            int gt = raw.indexOf('>');
                            payload.put("fromName", raw.substring(0, lt).trim());
                            payload.put("fromEmail", raw.substring(lt + 1, gt).trim());
                        } else {
                            payload.put("fromEmail", raw);
                            payload.put("fromName", "");
                        }
                    } else if ("Date".equalsIgnoreCase(name)) {
                        payload.put("date", header.get("value"));
                    }
                }
            }
        }

        return payload;
    }

    private static String asStr(Object obj) {
        return obj != null ? String.valueOf(obj) : null;
    }
}
