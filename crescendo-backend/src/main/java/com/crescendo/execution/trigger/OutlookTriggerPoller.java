package com.crescendo.execution.trigger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Polling trigger for Microsoft Outlook — checks for new emails via Microsoft Graph API.
 *
 * <h3>Personal Account OData Limitation — the root cause of months of debugging</h3>
 *
 * <p>For personal @outlook.com / @hotmail.com accounts, Microsoft Graph silently returns
 * {@code value=[]} (HTTP 200, no error) for ANY of these OData operations:
 * <ul>
 *   <li>{@code $filter=receivedDateTime ge ...} — silently returns []</li>
 *   <li>{@code $filter=receivedDateTime ge ... and receivedDateTime lt ...} — silently returns []</li>
 *   <li>{@code $filter=contains(subject, ...)} — silently returns []</li>
 *   <li>{@code $filter=from/emailAddress/address eq ...} — silently returns []</li>
 *   <li>{@code $filter} combined with {@code $orderby} — silently returns []</li>
 * </ul>
 *
 * <h3>Solution (no OData filter at all)</h3>
 * <ol>
 *   <li>Fetch messages using ONLY {@code $top} — no {@code $filter}, no {@code $orderby}.</li>
 *   <li>Graph returns messages in default order (newest first — receivedDateTime desc).
 *       This is the documented default behaviour and does not require an explicit $orderby.</li>
 *   <li>Stop paginating as soon as we hit a message older than {@code lastPollTime}.
 *       This is efficient: we never fetch more than we need.</li>
 *   <li>Apply sender, subject, and timestamp filters entirely client-side in Java.</li>
 * </ol>
 *
 * <p>This is equivalent to how n8n handles its "test/manual" mode and how ActivePieces
 * handles the first poll — the key insight being that server-side date filtering is
 * unreliable for personal accounts.
 */
@Component
public class OutlookTriggerPoller implements TriggerPoller {

    private static final Logger logger = LoggerFactory.getLogger(OutlookTriggerPoller.class);
    private static final String GRAPH_BASE = "https://graph.microsoft.com/v1.0";
    private static final int PAGE_SIZE = 50;
    private static final int MAX_PAGES = 10; // 500 emails max per poll

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
        logger.debug("[outlook-poller] Configuration: {}", configuration);

        // Truncate to seconds for consistent comparison with Graph API timestamps
        Instant cursor = lastPollTime.truncatedTo(ChronoUnit.SECONDS);

        // ── Client-side filters from step configuration ────────────────────────────
        String fromFilter = asStr(configuration != null ? configuration.get("fromFilter") : null);
        if (fromFilter == null) {
            fromFilter = asStr(configuration != null ? configuration.get("senderEmail") : null);
        }
        String subjectFilter = asStr(configuration != null ? configuration.get("subjectFilter") : null);

        // ── Endpoint ─────────────────────────────────────────────────────────────
        // Use /me/messages (all folders) so emails landing in Junk/Other are found.
        // The specific folderId from config is intentionally ignored for polling
        // because Graph's $filter breaks silently for personal accounts anyway.
        String endpoint = GRAPH_BASE + "/me/messages";

        // ── NO $filter — personal accounts ignore it silently ─────────────────────
        // We MUST use $orderby=receivedDateTime%20desc because default order is NOT guaranteed.
        // The Microsoft API limitation only applies when combining $filter and $orderby.
        // Using $orderby WITHOUT $filter works perfectly fine for personal accounts.
        String initialUrl = endpoint
                + "?$top=" + PAGE_SIZE
                + "&$orderby=receivedDateTime%20desc"
                + "&$select=id,subject,from,toRecipients,receivedDateTime,bodyPreview,hasAttachments";

        logger.info("[outlook-poller] Polling since '{}' | fromFilter='{}' | subjectFilter='{}'",
                cursor, fromFilter, subjectFilter);
        logger.debug("[outlook-poller] URL: {}", initialUrl);

        List<Map<String, Object>> results = new ArrayList<>();
        String nextUrl = initialUrl;
        int page = 0;
        boolean reachedOldMessages = false;

        while (nextUrl != null && page < MAX_PAGES && !reachedOldMessages) {
            page++;
            Map<String, Object> response = fetchPage(nextUrl, accessToken);
            if (response == null) break;

            Object valueObj = response.get("value");
            if (!(valueObj instanceof List)) break;

            List<Map<String, Object>> pageItems = (List<Map<String, Object>>) valueObj;
            logger.info("[outlook-poller] Page {}: {} message(s) returned", page, pageItems.size());

            if (pageItems.isEmpty()) break;

            for (Map<String, Object> msg : pageItems) {
                // ── Timestamp check (client-side) ──────────────────────────────────
                // Graph returns messages newest-first. Once we hit one older than cursor,
                // all subsequent messages will also be older — stop paginating.
                String receivedAt = asStr(msg.get("receivedDateTime"));
                if (receivedAt == null) continue;

                Instant msgTime;
                try {
                    msgTime = Instant.parse(receivedAt).truncatedTo(ChronoUnit.SECONDS);
                } catch (Exception e) {
                    logger.warn("[outlook-poller] Could not parse receivedDateTime '{}' — skipping", receivedAt);
                    continue;
                }

                if (!msgTime.isAfter(cursor)) {
                    // This message is AT or BEFORE the cursor — we're done
                    logger.info("[outlook-poller] Hit message at '{}' ≤ cursor '{}' — stopping pagination", msgTime, cursor);
                    reachedOldMessages = true;
                    break;
                }

                // ── Sender filter (exact, case-insensitive) ────────────────────────
                String fromEmail = extractFromEmail(msg);
                if (fromFilter != null) {
                    if (fromEmail == null || !fromEmail.equalsIgnoreCase(fromFilter)) {
                        logger.info("[outlook-poller] Skip '{}' — from='{}' != '{}'",
                                msg.get("subject"), fromEmail, fromFilter);
                        continue;
                    }
                }

                // ── Subject filter (case-insensitive contains) ─────────────────────
                String subject = asStr(msg.get("subject"));
                if (subjectFilter != null) {
                    if (subject == null || !subject.toLowerCase().contains(subjectFilter.toLowerCase())) {
                        logger.info("[outlook-poller] Skip — subject '{}' doesn't contain '{}'",
                                subject, subjectFilter);
                        continue;
                    }
                }

                // ── Build output payload ───────────────────────────────────────────
                Map<String, Object> payload = new HashMap<>();
                payload.put("id",               msg.get("id"));
                payload.put("subject",          subject);
                payload.put("bodyPreview",      msg.get("bodyPreview"));
                payload.put("receivedDateTime", receivedAt);
                payload.put("hasAttachments",   msg.get("hasAttachments"));
                payload.put("fromEmail",        fromEmail);
                payload.put("fromName",         extractFromName(msg));

                logger.info("[outlook-poller] ✓ Matched — subject='{}', from='{}', receivedAt='{}'",
                        subject, fromEmail, receivedAt);
                results.add(payload);
            }

            nextUrl = reachedOldMessages ? null : asStr(response.get("@odata.nextLink"));
        }

        logger.info("[outlook-poller] Done after {} page(s): {} message(s) matched", page, results.size());
        return results;
    }

    // ── HTTP ──────────────────────────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, Object> fetchPage(String url, String accessToken) {
        try {
            Map response = restClient.get()
                    .uri(URI.create(url))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header("Accept", "application/json")
                    .retrieve()
                    .body(Map.class);
            if (response != null) {
                logger.debug("[outlook-poller] Response keys: {}", response.keySet());
            }
            return (Map<String, Object>) response;
        } catch (Exception e) {
            logger.error("[outlook-poller] HTTP error on '{}': {} — {}",
                    url, e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String extractFromEmail(Map<String, Object> msg) {
        try {
            Map<String, Object> from = (Map<String, Object>) msg.get("from");
            if (from == null) return null;
            Map<String, Object> addr = (Map<String, Object>) from.get("emailAddress");
            return addr != null ? asStr(addr.get("address")) : null;
        } catch (Exception e) { return null; }
    }

    @SuppressWarnings("unchecked")
    private String extractFromName(Map<String, Object> msg) {
        try {
            Map<String, Object> from = (Map<String, Object>) msg.get("from");
            if (from == null) return null;
            Map<String, Object> addr = (Map<String, Object>) from.get("emailAddress");
            return addr != null ? asStr(addr.get("name")) : null;
        } catch (Exception e) { return null; }
    }

    private static String asStr(Object obj) {
        if (obj == null) return null;
        String s = String.valueOf(obj).trim();
        return s.isEmpty() ? null : s;
    }
}
