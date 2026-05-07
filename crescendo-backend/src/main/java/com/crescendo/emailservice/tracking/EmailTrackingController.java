package com.crescendo.emailservice.tracking;

import com.crescendo.emailservice.email_log.EmailLog;
import com.crescendo.emailservice.email_log.EmailLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Public tracking endpoints — whitelisted in SecurityConfig under /t/**.
 *
 *   GET /t/o/{emailId}          — open-tracking pixel (returns 1×1 transparent GIF)
 *   GET /t/c/{emailId}?url=...  — click-tracking redirect (validates URL, then 302)
 *
 * Tracking failures are silently swallowed — a broken tracker must never prevent
 * the user from reaching their link.
 */
@RestController
@RequestMapping("/t")
public class EmailTrackingController {

    private static final Logger logger = LoggerFactory.getLogger(EmailTrackingController.class);

    // 43-byte minimal 1×1 transparent GIF (GIF89a spec)
    private static final byte[] TRANSPARENT_GIF = {
            0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00,
            0x01, 0x00, (byte) 0x80, 0x00, 0x00, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, 0x00, 0x00, 0x00, 0x21,
            (byte) 0xF9, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x2C, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01,
            0x00, 0x00, 0x02, 0x02, 0x44, 0x01, 0x00, 0x3B
    };

    private final EmailLogRepository emailLogRepo;

    public EmailTrackingController(EmailLogRepository emailLogRepo) {
        this.emailLogRepo = emailLogRepo;
    }

    /**
     * Open-tracking pixel.
     * Records the first open time and increments the open counter, then
     * returns a 1×1 transparent GIF with no-cache headers so proxies
     * cannot suppress tracking by caching the response.
     */
    @GetMapping(value = "/o/{emailId}", produces = MediaType.IMAGE_GIF_VALUE)
    public ResponseEntity<byte[]> trackOpen(@PathVariable UUID emailId) {
        try {
            Optional<EmailLog> logOpt = emailLogRepo.findById(emailId);
            logOpt.ifPresent(log -> {
                log.setOpenCount(log.getOpenCount() + 1);
                if (log.getOpenedAt() == null) {
                    log.setOpenedAt(Instant.now());
                }
                emailLogRepo.save(log);
            });
        } catch (Exception e) {
            logger.debug("[tracking] Failed to record open for emailId={}: {}", emailId, e.getMessage());
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .contentType(MediaType.IMAGE_GIF)
                .body(TRANSPARENT_GIF);
    }

    /**
     * Click-tracking redirect.
     * Validates the destination URL (must be http/https, must not target private/local networks),
     * increments the click counter, then issues a 302 redirect to the original URL.
     *
     * Security: the URL is validated before following to prevent open-redirect abuse
     * and SSRF against internal services.
     */
    @GetMapping("/c/{emailId}")
    public ResponseEntity<Void> trackClick(@PathVariable UUID emailId,
                                           @RequestParam String url) {
        URI target = validateRedirectTarget(url);
        if (target == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Optional<EmailLog> logOpt = emailLogRepo.findById(emailId);
            logOpt.ifPresent(log -> {
                log.setClickCount(log.getClickCount() + 1);
                emailLogRepo.save(log);
            });
        } catch (Exception e) {
            logger.debug("[tracking] Failed to record click for emailId={}: {}", emailId, e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(target)
                .build();
    }

    /**
     * Validates that the redirect target is a safe external http/https URL.
     *
     * Rejects:
     *  - Non-http/https schemes (javascript:, file:, data:, etc.)
     *  - Localhost and loopback (localhost, 127.0.0.1, ::1)
     *  - RFC-1918 private ranges (10.x, 172.16–31.x, 192.168.x)
     *  - Link-local (169.254.x) and unspecified (0.0.0.0)
     *
     * @return the validated URI, or null if the URL is unsafe
     */
    private static URI validateRedirectTarget(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return null;
            }
            String host = uri.getHost();
            if (host == null) return null;
            String lower = host.toLowerCase();
            if (lower.equals("localhost")
                    || lower.equals("127.0.0.1")
                    || lower.equals("::1")
                    || lower.equals("0.0.0.0")
                    || lower.startsWith("10.")
                    || lower.startsWith("192.168.")
                    || lower.startsWith("169.254.")) {
                return null;
            }
            // Block 172.16.0.0/12
            if (lower.startsWith("172.")) {
                String[] parts = lower.split("\\.");
                if (parts.length >= 2) {
                    try {
                        int second = Integer.parseInt(parts[1]);
                        if (second >= 16 && second <= 31) return null;
                    } catch (NumberFormatException ignored) {}
                }
            }
            return uri;
        } catch (Exception e) {
            return null;
        }
    }
}
