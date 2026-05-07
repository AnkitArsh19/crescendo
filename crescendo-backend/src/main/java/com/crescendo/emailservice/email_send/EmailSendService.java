package com.crescendo.emailservice.email_send;

import com.crescendo.emailservice.email_log.EmailLog;
import com.crescendo.emailservice.email_log.EmailLogRepository;
import com.crescendo.emailservice.emailtemplate.template_command.EmailTemplate_command;
import com.crescendo.emailservice.emailtemplate.template_command.EmailTemplate_commandRepository;
import com.crescendo.emailservice.suppression.EmailSuppressionService;
import com.crescendo.emailservice.tracking.TrackingInjector;
import com.crescendo.enums.EmailStatus;
import com.crescendo.shared.infrastructure.event.RedisDomainEventPublisher;
import com.crescendo.shared.util.TemplateInterpolator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for sending emails via the public API (API-key authenticated).
 * Creates an EmailLog record and enqueues the email for async delivery.
 *
 * Pre-send pipeline (in order):
 *   1. Resolve template if templateId provided (interpolate variables)
 *   2. Validate subject + body are present
 *   3. Check suppression list — if suppressed, log as SUPPRESSED and return immediately
 *   4. Inject open-tracking pixel (if trackingBaseUrl configured)
 *   5. Rewrite click links (if trackingBaseUrl configured)
 *   6. Build List-Unsubscribe header (if trackingBaseUrl configured)
 *   7. Save EmailLog as PENDING and enqueue
 */
@Service
public class EmailSendService {

    private static final Logger logger = LoggerFactory.getLogger(EmailSendService.class);

    private final EmailLogRepository emailLogRepo;
    private final EmailTemplate_commandRepository templateRepo;
    private final RedisDomainEventPublisher redisDomainEventPublisher;
    private final EmailSuppressionService suppressionService;

    @Value("${app.tracking.base-url:}")
    private String trackingBaseUrl;

    public EmailSendService(EmailLogRepository emailLogRepo,
                            EmailTemplate_commandRepository templateRepo,
                            RedisDomainEventPublisher redisDomainEventPublisher,
                            EmailSuppressionService suppressionService) {
        this.emailLogRepo = emailLogRepo;
        this.templateRepo = templateRepo;
        this.redisDomainEventPublisher = redisDomainEventPublisher;
        this.suppressionService = suppressionService;
    }

    public EmailSendDto.SendEmailResponse sendEmail(UUID userId, UUID apiKeyId,
                                                     EmailSendDto.SendEmailRequest req) {
        // 1. Resolve template
        String subject;
        String htmlBody;
        String textBody;

        if (req.templateId() != null) {
            EmailTemplate_command template = templateRepo.findByIdAndUserId(req.templateId(), userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Template not found: " + req.templateId()));

            Map<String, Object> data = req.templateData() != null ? req.templateData() : Map.of();
            subject = TemplateInterpolator.interpolate(template.getSubject(), data);
            htmlBody = TemplateInterpolator.interpolate(template.getHTMLBody(), data);
            textBody = template.getTextBody() != null
                    ? TemplateInterpolator.interpolate(template.getTextBody(), data)
                    : null;
        } else {
            subject = req.subject();
            htmlBody = req.htmlBody();
            textBody = req.textBody();
        }

        // 2. Validate
        if (subject == null || subject.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Subject is required (either directly or via a template)");
        }
        if ((htmlBody == null || htmlBody.isBlank()) && (textBody == null || textBody.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Email body is required — provide htmlBody, textBody, or a templateId");
        }

        UUID emailId = UUID.randomUUID();

        // 3. Suppression check — create log and return without queuing
        if (suppressionService.isSuppressed(userId, req.to())) {
            EmailLog suppressed = new EmailLog(emailId, userId, apiKeyId, req.from(), req.to(), subject, EmailStatus.SUPPRESSED);
            if (req.templateId() != null) suppressed.setTemplateId(req.templateId());
            emailLogRepo.save(suppressed);
            logger.info("[email-send] Email {} suppressed for to={}", emailId, req.to());
            return new EmailSendDto.SendEmailResponse(emailId, req.to(), req.from(), subject, EmailStatus.SUPPRESSED.name());
        }

        // 4 & 5. Inject tracking into HTML (only when base URL is configured)
        boolean trackingEnabled = trackingBaseUrl != null && !trackingBaseUrl.isBlank();
        if (trackingEnabled && htmlBody != null) {
            htmlBody = TrackingInjector.rewriteClickLinks(htmlBody, emailId, trackingBaseUrl);
            htmlBody = TrackingInjector.injectOpenPixel(htmlBody, emailId, trackingBaseUrl);
        }

        // 6. Build List-Unsubscribe header
        String listUnsubscribeHeader = trackingEnabled
                ? "<" + trackingBaseUrl + "/unsubscribe?token=" + emailId + ">"
                : null;

        // 7. Save log as PENDING and enqueue
        EmailLog log = new EmailLog(emailId, userId, apiKeyId, req.from(), req.to(), subject, EmailStatus.PENDING);
        if (req.templateId() != null) log.setTemplateId(req.templateId());
        emailLogRepo.save(log);

        Map<String, String> emailData = new HashMap<>();
        emailData.put("emailId", emailId.toString());
        emailData.put("userId", userId.toString());
        emailData.put("apiKeyId", apiKeyId.toString());
        emailData.put("to", req.to());
        emailData.put("from", req.from());
        emailData.put("subject", subject);
        if (htmlBody != null) emailData.put("htmlBody", htmlBody);
        if (textBody != null) emailData.put("textBody", textBody);
        if (listUnsubscribeHeader != null) emailData.put("listUnsubscribeHeader", listUnsubscribeHeader);

        redisDomainEventPublisher.enqueueEmail(emailData);

        logger.info("[email-send] Enqueued email {} for user {} via API key {}", emailId, userId, apiKeyId);

        return new EmailSendDto.SendEmailResponse(emailId, req.to(), req.from(), subject, EmailStatus.PENDING.name());
    }
}
