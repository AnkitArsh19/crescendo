package com.crescendo.emailservice.webhook;

import com.crescendo.emailservice.domain_event.EmailBouncedEvent;
import com.crescendo.emailservice.domain_event.EmailDeliveredEvent;
import com.crescendo.emailservice.email_log.EmailLog;
import com.crescendo.emailservice.email_log.EmailLogRepository;
import com.crescendo.emailservice.suppression.EmailSuppressionService;
import com.crescendo.enums.EmailStatus;
import com.crescendo.shared.infrastructure.event.RedisDomainEventPublisher;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.UUID;

/**
 * Public webhook endpoint for email provider delivery callbacks.
 * Receives delivery/bounce notifications from SMTP relays or third-party
 * email providers and updates EmailLog status accordingly.
 *
 * This endpoint is publicly accessible (no auth) because providers cannot
 * authenticate — it is whitelisted in SecurityConfig under /webhooks/**.
 *
 * Expected payload:
 *   POST /webhooks/email-events
 *   {
 *     "type": "delivered" | "bounced" | "failed",
 *     "emailId": "uuid",                       // our internal ID
 *     "providerMessageId": "provider-msg-id",   // fallback lookup key
 *     "reason": "optional bounce/failure reason"
 *   }
 */
@RestController
@RequestMapping("/webhooks/email-events")
public class EmailEventWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(EmailEventWebhookController.class);

    private final EmailLogRepository emailLogRepo;
    private final RedisDomainEventPublisher eventPublisher;
    private final EmailSuppressionService suppressionService;
    private final ObjectMapper objectMapper;

    @Value("${credentials.crypto.key:}")
    private String webhookSecret;

    public EmailEventWebhookController(EmailLogRepository emailLogRepo,
                                       RedisDomainEventPublisher eventPublisher,
                                       EmailSuppressionService suppressionService,
                                       ObjectMapper objectMapper) {
        this.emailLogRepo = emailLogRepo;
        this.eventPublisher = eventPublisher;
        this.suppressionService = suppressionService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, String>> handleEmailEvent(
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @RequestBody String rawBody) {

        // Verify HMAC signature if webhook secret is configured
        if (webhookSecret != null && !webhookSecret.isBlank()) {
            if (signature == null || signature.isBlank()) {
                logger.warn("[email-webhook] Rejected: missing X-Webhook-Signature header");
                return ResponseEntity.status(403).body(Map.of("error", "Missing webhook signature"));
            }
            if (!verifySignature(rawBody, signature)) {
                logger.warn("[email-webhook] Rejected: invalid X-Webhook-Signature");
                return ResponseEntity.status(403).body(Map.of("error", "Invalid webhook signature"));
            }
        }

        // Parse the JSON body into a map
        Map<String, String> payload;
        try {
            Map<String, String> parsed = objectMapper.readValue(rawBody, Map.class);
            payload = parsed;
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid JSON payload"));
        }
        String type = payload.get("type");
        String emailIdStr = payload.get("emailId");
        String providerMessageId = payload.get("providerMessageId");
        String reason = payload.get("reason");

        if (type == null || type.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing 'type' field"));
        }

        // Resolve the EmailLog — first by our ID, then fall back to providerMessageId
        Optional<EmailLog> logOpt = Optional.empty();
        if (emailIdStr != null && !emailIdStr.isBlank()) {
            try {
                logOpt = emailLogRepo.findById(UUID.fromString(emailIdStr));
            } catch (IllegalArgumentException ignored) {
                // invalid UUID — fall through to providerMessageId lookup
            }
        }
        if (logOpt.isEmpty() && providerMessageId != null && !providerMessageId.isBlank()) {
            logOpt = emailLogRepo.findByProviderMessageId(providerMessageId);
        }

        if (logOpt.isEmpty()) {
            logger.warn("[email-webhook] Email not found for event: type={}, emailId={}, providerMessageId={}",
                    type, emailIdStr, providerMessageId);
            return ResponseEntity.ok(Map.of("status", "ignored", "reason", "email not found"));
        }

        EmailLog log = logOpt.get();

        switch (type.toLowerCase()) {
            case "delivered" -> {
                if (log.getStatus() == EmailStatus.SENT) {
                    log.setStatus(EmailStatus.DELIVERED);
                    emailLogRepo.save(log);
                    eventPublisher.publish(new EmailDeliveredEvent(log.getId()));
                    logger.info("[email-webhook] Email {} marked as DELIVERED", log.getId());
                }
            }
            case "bounced" -> {
                if (log.getStatus() == EmailStatus.SENT || log.getStatus() == EmailStatus.DELIVERED) {
                    log.setStatus(EmailStatus.BOUNCED);
                    log.setError(reason != null ? truncate(reason, 2000) : "Bounced (no reason provided)");
                    emailLogRepo.save(log);
                    eventPublisher.publish(new EmailBouncedEvent(log.getId(), log.getError()));
                    // Auto-suppress bounced addresses so they are skipped on future sends
                    suppressionService.suppress(log.getUserId(), log.getToAddress(), "BOUNCED");
                    logger.info("[email-webhook] Email {} marked as BOUNCED: {}", log.getId(), log.getError());
                }
            }
            case "failed" -> {
                if (log.getStatus() != EmailStatus.FAILED) {
                    log.setStatus(EmailStatus.FAILED);
                    log.setError(reason != null ? truncate(reason, 2000) : "Failed (no reason provided)");
                    emailLogRepo.save(log);
                    logger.info("[email-webhook] Email {} marked as FAILED: {}", log.getId(), log.getError());
                }
            }
            default -> {
                logger.debug("[email-webhook] Ignoring unknown event type '{}' for email {}", type, log.getId());
                return ResponseEntity.ok(Map.of("status", "ignored", "reason", "unknown event type"));
            }
        }

        return ResponseEntity.ok(Map.of("status", "processed"));
    }

    private String truncate(String value, int maxLen) {
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }

    /**
     * Verifies the HMAC-SHA256 signature of the incoming webhook payload.
     * The expected format of the signature header is the hex-encoded HMAC.
     */
    private boolean verifySignature(String payload, String receivedSignature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            byte[] keyBytes = Base64.getDecoder().decode(webhookSecret);
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expected = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    receivedSignature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.error("[email-webhook] Signature verification error: {}", e.getMessage());
            return false;
        }
    }
}
