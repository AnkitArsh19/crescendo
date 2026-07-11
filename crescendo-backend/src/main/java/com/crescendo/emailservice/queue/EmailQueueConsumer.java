package com.crescendo.emailservice.queue;

import com.crescendo.emailservice.email_log.EmailLog;
import com.crescendo.emailservice.email_log.EmailLogRepository;
import com.crescendo.emailservice.provider.EmailMessage;
import com.crescendo.emailservice.provider.EmailProviderResolver;
import com.crescendo.emailservice.provider.EmailSendResult;
import com.crescendo.enums.EmailStatus;
import com.crescendo.config.RedisStreamConfig;
import com.crescendo.shared.infrastructure.lock.DistributedLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis Stream consumer for the email sending queue ({@code crescendo:queue:email}).
 *
 * Flow:
 * <ol>
 *   <li>Dequeue email message from Redis Stream</li>
 *   <li>Acquire distributed lock on the email ID (prevent duplicate sends)</li>
 *   <li>Look up the EmailLog record and verify it's still PENDING</li>
 *   <li>Send via the active {@link EmailProvider} (SMTP or fallback)</li>
 *   <li>Update EmailLog: PENDING → SENT (success) or PENDING → FAILED (error)</li>
 *   <li>Release lock</li>
 * </ol>
 *
 * Registered in {@link com.crescendo.shared.infrastructure.stream.StreamConsumerRegistrar}.
 */
@Component
public class EmailQueueConsumer implements StreamListener<String, MapRecord<String, Object, Object>> {

    private static final Logger logger = LoggerFactory.getLogger(EmailQueueConsumer.class);

    private final EmailLogRepository emailLogRepo;
    private final EmailProviderResolver emailProviderResolver;
    private final DistributedLockService lockService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final com.crescendo.emailservice.email_send.SendEligibilityService eligibilityService;
    private final com.crescendo.emailservice.suppression.EmailSuppressionService suppressionService;
    private final String trackingBaseUrl;

    public EmailQueueConsumer(EmailLogRepository emailLogRepo,
                              EmailProviderResolver emailProviderResolver,
                              DistributedLockService lockService,
                              RedisTemplate<String, Object> redisTemplate,
                              com.crescendo.emailservice.email_send.SendEligibilityService eligibilityService,
                              com.crescendo.emailservice.suppression.EmailSuppressionService suppressionService,
                              @org.springframework.beans.factory.annotation.Value("${app.tracking.base-url:}") String trackingBaseUrl) {
        this.emailLogRepo = emailLogRepo;
        this.emailProviderResolver = emailProviderResolver;
        this.lockService = lockService;
        this.redisTemplate = redisTemplate;
        this.eligibilityService = eligibilityService;
        this.suppressionService = suppressionService;
        this.trackingBaseUrl = trackingBaseUrl;
    }

    @Override
    public void onMessage(MapRecord<String, Object, Object> message) {
        Map<Object, Object> raw = message.getValue();

        String emailId = unquote(raw.get("emailId"));
        String to = unquote(raw.get("to"));
        String from = unquote(raw.get("from"));
        String subject = unquote(raw.get("subject"));
        String htmlBody = unquote(raw.get("htmlBody"));
        String textBody = unquote(raw.get("textBody"));
        String listUnsubscribeHeader = unquote(raw.get("listUnsubscribeHeader"));

        if (emailId == null || to == null || from == null) {
            logger.warn("[email-queue] Malformed email message, missing required fields: {}", raw);
            acknowledge(message);
            return;
        }

        logger.info("[email-queue] Dequeued email {} to={}", emailId, to);

        String lockKey = "email-send:" + emailId;
        Optional<String> lockToken = lockService.tryLock(lockKey, 60_000); // 1 min TTL

        if (lockToken.isEmpty()) {
            logger.warn("[email-queue] Could not acquire lock for email {}, skipping (likely duplicate)", emailId);
            return;
        }

        try {
            processEmail(emailId, to, from, subject, htmlBody, textBody, listUnsubscribeHeader);
            acknowledge(message);
        } finally {
            lockService.unlock(lockKey, lockToken.get());
        }
    }

    private void processEmail(String emailId, String to, String from,
                              String subject, String htmlBody, String textBody,
                              String listUnsubscribeHeader) {
        UUID id = UUID.fromString(emailId);

        EmailLog log = emailLogRepo.findById(id).orElse(null);
        if (log == null) {
            logger.warn("[email-queue] EmailLog not found for {}", emailId);
            return;
        }

        if (log.getStatus() != EmailStatus.PENDING) {
            logger.info("[email-queue] Email {} is already {}, skipping", emailId, log.getStatus());
            return;
        }

        var eligibility = eligibilityService.checkEligibility(log.getUserId(), from, to, log.getEmailType());
        if (!eligibility.eligible()) {
            logger.warn("[email-queue] Email {} is not eligible to send: {}", emailId, eligibility.reason());
            EmailStatus failStatus = eligibility.suggestedStatus().equals("SUPPRESSED") ? EmailStatus.SUPPRESSED : EmailStatus.FAILED;
            log.setStatus(failStatus);
            log.setError(eligibility.reason());
            emailLogRepo.save(log);
            return;
        }

        boolean trackingEnabled = eligibility.domain() != null && eligibility.domain().isTrackingEnabled()
                && trackingBaseUrl != null && !trackingBaseUrl.isBlank();

        if (trackingEnabled && htmlBody != null) {
            htmlBody = com.crescendo.emailservice.tracking.TrackingInjector.rewriteClickLinks(htmlBody, id, trackingBaseUrl);
            htmlBody = com.crescendo.emailservice.tracking.TrackingInjector.injectOpenPixel(htmlBody, id, trackingBaseUrl);
        }

        String secureToken = suppressionService.generateUnsubscribeToken(id);
        String finalUnsubscribeHeader = listUnsubscribeHeader != null ? listUnsubscribeHeader :
                (trackingEnabled ? "<" + trackingBaseUrl + "/unsubscribe?token=" + secureToken + ">" : null);

        EmailMessage emailMessage = new EmailMessage(to, from, subject, htmlBody, textBody, finalUnsubscribeHeader, log.getEmailType(), log.getId().toString());

        com.crescendo.emailservice.provider.EmailProvider emailProvider = emailProviderResolver.resolve(eligibility.domain());
        try {
            EmailSendResult result = emailProvider.send(emailMessage);

            if (result.success()) {
                log.setStatus(EmailStatus.SENT);
                log.setProviderMessageId(result.providerMessageId());
                log.setProvider(emailProvider.providerName());
                log.setSentAt(Instant.now());
                emailLogRepo.save(log);

                logger.info("[email-queue] Email {} sent successfully via {} (providerMsgId={})",
                        emailId, emailProvider.providerName(), result.providerMessageId());
            } else {
                log.setStatus(EmailStatus.FAILED);
                log.setError(result.error());
                log.setProvider(emailProvider.providerName());
                emailLogRepo.save(log);

                logger.error("[email-queue] Email {} failed: {}", emailId, result.error());
            }
        } catch (Exception e) {
            log.setStatus(EmailStatus.FAILED);
            log.setError("Unexpected error: " + e.getMessage());
            log.setProvider(emailProvider.providerName());
            emailLogRepo.save(log);

            logger.error("[email-queue] Uncaught exception sending email {}: {}", emailId, e.getMessage(), e);
        }
    }

    private void acknowledge(MapRecord<String, Object, Object> message) {
        try {
            redisTemplate.opsForStream().acknowledge(
                    RedisStreamConfig.STREAM_EMAIL_QUEUE,
                    RedisStreamConfig.CONSUMER_GROUP,
                    message.getId());
        } catch (Exception e) {
            logger.error("[email-queue] Failed to ACK message {}: {}", message.getId(), e.getMessage());
        }
    }

    private static String unquote(Object value) {
        if (value == null) return null;
        String s = value.toString();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
