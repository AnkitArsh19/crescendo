package com.crescendo.emailservice.tracking;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Injects engagement-tracking hooks into outgoing HTML email bodies.
 *
 * Two features:
 *   1. Open tracking  — appends a 1×1 transparent image whose src points to
 *                       /t/o/{emailId}. The server records the open on first request.
 *   2. Click tracking — rewrites href="http(s)://..." links to go through
 *                       /t/c/{emailId}?url=<encoded>, where the click is recorded
 *                       before the user is forwarded to the original destination.
 *
 * Only http/https links are rewritten — mailto, tel, and anchor (#) links are left
 * untouched. All rewriting is skipped when baseUrl is null or blank.
 */
public final class TrackingInjector {

    private TrackingInjector() {}

    private static final Pattern HREF_PATTERN =
            Pattern.compile("href=\"(https?://[^\"]+)\"", Pattern.CASE_INSENSITIVE);

    /**
     * Appends a 1×1 tracking pixel immediately before the closing {@code </body>} tag.
     * If no {@code </body>} is found, appends to the end of the document.
     */
    public static String injectOpenPixel(String html, UUID emailId, String baseUrl) {
        String pixelUrl = baseUrl + "/t/o/" + emailId;
        String pixel = "<img src=\"" + pixelUrl
                + "\" width=\"1\" height=\"1\" alt=\"\" style=\"display:none\" />";
        int bodyClose = html.lastIndexOf("</body>");
        if (bodyClose >= 0) {
            return html.substring(0, bodyClose) + pixel + html.substring(bodyClose);
        }
        return html + pixel;
    }

    /**
     * Rewrites all {@code href="http(s)://..."} attributes so clicks pass through
     * the tracking redirect endpoint before reaching the original URL.
     *
     * Non-http/https links (mailto:, tel:, #, etc.) are never modified.
     */
    public static String rewriteClickLinks(String html, UUID emailId, String baseUrl) {
        Matcher m = HREF_PATTERN.matcher(html);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String originalUrl = m.group(1);
            String encoded = URLEncoder.encode(originalUrl, StandardCharsets.UTF_8);
            String trackingUrl = baseUrl + "/t/c/" + emailId + "?url=" + encoded;
            m.appendReplacement(sb, "href=\"" + trackingUrl + "\"");
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
