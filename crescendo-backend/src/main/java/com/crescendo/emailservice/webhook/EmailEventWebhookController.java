package com.crescendo.emailservice.webhook;

import com.crescendo.emailservice.domain_event.EmailBouncedEvent;
import com.crescendo.emailservice.domain_event.EmailDeliveredEvent;
import com.crescendo.emailservice.email_log.EmailLog;
import com.crescendo.emailservice.email_log.EmailLogRepository;
import com.crescendo.emailservice.suppression.EmailSuppressionService;
import com.crescendo.emailservice.domain_event.EmailComplainedEvent;
import com.crescendo.emailservice.domain.Domain;
import com.crescendo.emailservice.domain.DomainRepository;
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
    private final DomainRepository domainRepo;
    private final RedisDomainEventPublisher eventPublisher;
    private final EmailSuppressionService suppressionService;
    private final ObjectMapper objectMapper;

    @Value("${credentials.crypto.key:}")
    private String webhookSecret;

    public EmailEventWebhookController(EmailLogRepository emailLogRepo,
                                       DomainRepository domainRepo,
                                       RedisDomainEventPublisher eventPublisher,
                                       EmailSuppressionService suppressionService,
                                       ObjectMapper objectMapper) {
        this.emailLogRepo = emailLogRepo;
        this.domainRepo = domainRepo;
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
        UUID domainId = resolveDomainId(log.getFromAddress(), log.getUserId());

        switch (type.toLowerCase()) {
            case "delivered" -> {
                if (log.getStatus() == com.crescendo.enums.EmailStatus.SENT) {
                    log.setStatus(com.crescendo.enums.EmailStatus.DELIVERED);
                    emailLogRepo.save(log);
                    eventPublisher.publish(new EmailDeliveredEvent(log.getId(), domainId));
                    suppressionService.recordDelivery(log.getUserId(), log.getToAddress());
                    logger.info("[email-webhook] Email {} marked as DELIVERED", log.getId());
                }
            }
            case "hard_bounce", "bounced" -> {
                if (log.getStatus() == com.crescendo.enums.EmailStatus.SENT || log.getStatus() == com.crescendo.enums.EmailStatus.DELIVERED) {
                    log.setStatus(com.crescendo.enums.EmailStatus.BOUNCED);
                    log.setError(reason != null ? truncate(reason, 2000) : "Hard bounce");
                    emailLogRepo.save(log);
                    eventPublisher.publish(new EmailBouncedEvent(log.getId(), domainId, log.getError()));
                    suppressionService.handleHardBounce(log.getUserId(), log.getToAddress(), null);
                    logger.info("[email-webhook] Email {} marked as HARD_BOUNCED: {}", log.getId(), log.getError());
                }
            }
            case "soft_bounce" -> {
                if (log.getStatus() == com.crescendo.enums.EmailStatus.SENT || log.getStatus() == com.crescendo.enums.EmailStatus.DELIVERED) {
                    log.setError(reason != null ? truncate(reason, 2000) : "Soft bounce");
                    emailLogRepo.save(log);
                    suppressionService.handleSoftBounce(log.getUserId(), log.getToAddress(), null);
                    logger.info("[email-webhook] Email {} recorded SOFT_BOUNCE: {}", log.getId(), log.getError());
                }
            }
            case "failed" -> {
                if (log.getStatus() != com.crescendo.enums.EmailStatus.FAILED) {
                    log.setStatus(com.crescendo.enums.EmailStatus.FAILED);
                    log.setError(reason != null ? truncate(reason, 2000) : "Failed (no reason provided)");
                    emailLogRepo.save(log);
                    logger.info("[email-webhook] Email {} marked as FAILED: {}", log.getId(), log.getError());
                }
            }
            case "complaint", "spam" -> {
                if (log.getStatus() == com.crescendo.enums.EmailStatus.SENT || log.getStatus() == com.crescendo.enums.EmailStatus.DELIVERED) {
                    log.setStatus(com.crescendo.enums.EmailStatus.COMPLAINED);
                    log.setError(reason != null ? truncate(reason, 2000) : "Marked as spam by recipient");
                    emailLogRepo.save(log);
                    eventPublisher.publish(new EmailComplainedEvent(log.getId(), domainId, log.getError()));
                    suppressionService.handleComplaint(log.getUserId(), log.getToAddress(), null);
                    logger.warn("[email-webhook] Email {} resulted in COMPLAINT from {}", log.getId(), log.getToAddress());
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

    private UUID resolveDomainId(String fromAddress, UUID userId) {
        if (fromAddress == null || !fromAddress.contains("@")) return null;
        String domainName = fromAddress.split("@")[1];
        return domainRepo.findByDomainNameAndUserId(domainName, userId)
                .map(Domain::getId)
                .orElse(null);
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
