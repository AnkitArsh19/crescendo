package com.crescendo.emailservice.email_send;

import com.crescendo.emailservice.email_log.EmailLog;
import com.crescendo.emailservice.email_log.EmailLogRepository;
import com.crescendo.emailservice.emailtemplate.template_command.EmailTemplate_command;
import com.crescendo.emailservice.emailtemplate.template_command.EmailTemplate_commandRepository;
import com.crescendo.emailservice.suppression.EmailSuppressionService;
import com.crescendo.enums.EmailStatus;
import com.crescendo.config.RedisStreamConfig;
import com.crescendo.logbook.outbox.OutboxEvent;
import com.crescendo.logbook.outbox.OutboxEventRepository;
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
    private final OutboxEventRepository outboxRepo;
    private final EmailSuppressionService suppressionService;

    @Value("${app.tracking.base-url:}")
    private String trackingBaseUrl;

    public EmailSendService(EmailLogRepository emailLogRepo,
                            EmailTemplate_commandRepository templateRepo,
                            OutboxEventRepository outboxRepo,
                            EmailSuppressionService suppressionService) {
        this.emailLogRepo = emailLogRepo;
        this.templateRepo = templateRepo;
        this.outboxRepo = outboxRepo;
        this.suppressionService = suppressionService;
    }

    @org.springframework.transaction.annotation.Transactional
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

            if (template.getStatus() != com.crescendo.emailservice.emailtemplate.template_command.EmailTemplate_command.TemplateStatus.PUBLISHED || template.getPublishedVersionSnapshot() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template is not published: " + req.templateId());
            }

            Map<String, Object> data = req.templateData() != null ? req.templateData() : Map.of();
            var snapshot = template.getPublishedVersionSnapshot();
            subject = TemplateInterpolator.interpolate(snapshot.subject(), data);
            htmlBody = TemplateInterpolator.interpolate(snapshot.htmlBody(), data);
            textBody = snapshot.textBody() != null
                    ? TemplateInterpolator.interpolate(snapshot.textBody(), data)
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
            EmailLog suppressed = new EmailLog(emailId, userId, apiKeyId, req.from(), req.to(), subject, EmailStatus.SUPPRESSED, req.emailType());
            if (req.templateId() != null) suppressed.setTemplateId(req.templateId());
            emailLogRepo.save(suppressed);
            logger.info("[email-send] Email {} suppressed for to={}", emailId, req.to());
            return new EmailSendDto.SendEmailResponse(emailId, req.to(), req.from(), subject, EmailStatus.SUPPRESSED.name());
        }

        // 6. Build List-Unsubscribe header (moved to consumer)

        // 7. Save log as PENDING and enqueue
        EmailLog log = new EmailLog(emailId, userId, apiKeyId, req.from(), req.to(), subject, EmailStatus.PENDING, req.emailType());
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

        Map<String, Object> outboxData = new HashMap<>(emailData);
        outboxRepo.save(new OutboxEvent(
                UUID.randomUUID(),
                RedisStreamConfig.STREAM_EMAIL_QUEUE,
                outboxData
        ));

        logger.info("[email-send] Enqueued email {} for user {} via API key {}", emailId, userId, apiKeyId);

        return new EmailSendDto.SendEmailResponse(emailId, req.to(), req.from(), subject, EmailStatus.PENDING.name());
    }

    @org.springframework.transaction.annotation.Transactional
    public EmailSendDto.SendEmailResponse sendTemplated(UUID userId, UUID apiKeyId,
                                                        EmailSendDto.SendTemplatedRequest req) {
        EmailTemplate_command template = templateRepo.findByIdAndUserId(req.templateId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Template not found: " + req.templateId()));

        if (template.getStatus() != com.crescendo.emailservice.emailtemplate.template_command.EmailTemplate_command.TemplateStatus.PUBLISHED || template.getPublishedVersionSnapshot() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template is not published: " + req.templateId());
        }

        Map<String, Object> data = req.templateData() != null ? req.templateData() : Map.of();
        var snapshot = template.getPublishedVersionSnapshot();
        String subject = TemplateInterpolator.interpolate(snapshot.subject(), data);
        String htmlBody = TemplateInterpolator.interpolate(snapshot.htmlBody(), data);
        String textBody = snapshot.textBody() != null
                ? TemplateInterpolator.interpolate(snapshot.textBody(), data)
                : null;

        EmailSendDto.SendEmailRequest fullReq = new EmailSendDto.SendEmailRequest(
                req.from(), req.to(), subject, htmlBody, textBody, req.templateId(), req.templateData(), 
                req.emailType() != null ? req.emailType() : com.crescendo.enums.EmailType.TRANSACTIONAL
        );
        return sendEmail(userId, apiKeyId, fullReq);
    }

    @org.springframework.transaction.annotation.Transactional
    public EmailSendDto.SendBatchResponse sendBatch(UUID userId, UUID apiKeyId,
                                                    EmailSendDto.SendBatchRequest req) {
        if (req.to() == null || req.to().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one recipient is required");
        }
        if (req.to().size() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum batch size is 100");
        }

        java.util.List<EmailSendDto.SendEmailResponse> responses = new java.util.ArrayList<>();
        for (String to : req.to()) {
            EmailSendDto.SendEmailRequest singleReq = new EmailSendDto.SendEmailRequest(
                    req.from(), to, req.subject(), req.htmlBody(), req.textBody(), 
                    req.templateId(), req.templateData(), 
                    req.emailType() != null ? req.emailType() : com.crescendo.enums.EmailType.TRANSACTIONAL
            );
            responses.add(sendEmail(userId, apiKeyId, singleReq));
        }
        return new EmailSendDto.SendBatchResponse(responses);
    }
}
