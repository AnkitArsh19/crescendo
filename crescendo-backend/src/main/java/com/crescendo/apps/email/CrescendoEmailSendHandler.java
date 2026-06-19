package com.crescendo.apps.email;

import com.crescendo.emailservice.email_log.EmailLog;
import com.crescendo.emailservice.email_log.EmailLogRepository;
import com.crescendo.emailservice.emailtemplate.template_command.EmailTemplate_command;
import com.crescendo.emailservice.emailtemplate.template_command.EmailTemplate_commandRepository;
import com.crescendo.enums.EmailStatus;
import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import com.crescendo.logbook.outbox.OutboxEvent;
import com.crescendo.logbook.outbox.OutboxEventRepository;
import com.crescendo.config.RedisStreamConfig;
import com.crescendo.shared.util.TemplateInterpolator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sends emails via Crescendo's built-in email service.
 *
 * <p>Config: {@code to}, {@code from}, {@code subject}, {@code htmlBody}, {@code textBody}, {@code templateId}
 * <p>All string fields support {@code {{variable}}} interpolation from input data.
 */
@ActionMapping(appKey = "crescendo-email", actionKey = "send-email")
public class CrescendoEmailSendHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(CrescendoEmailSendHandler.class);

    private final EmailLogRepository emailLogRepo;
    private final EmailTemplate_commandRepository templateRepo;
    private final OutboxEventRepository outboxRepo;

    public CrescendoEmailSendHandler(EmailLogRepository emailLogRepo,
                                     EmailTemplate_commandRepository templateRepo,
                                     OutboxEventRepository outboxRepo) {
        this.emailLogRepo = emailLogRepo;
        this.templateRepo = templateRepo;
        this.outboxRepo = outboxRepo;
    }

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> input = context.inputData() != null ? context.inputData() : Map.of();

        String to = resolveString(config, "to", input);
        String from = resolveString(config, "from", input);

        if (to == null || to.isBlank()) {
            return ActionResult.failure("Email action requires 'to' in step configuration");
        }
        if (from == null || from.isBlank()) {
            return ActionResult.failure("Email action requires 'from' in step configuration");
        }

        String subject;
        String htmlBody;
        String textBody;

        String templateIdStr = config.get("templateId") != null ? config.get("templateId").toString() : null;
        UUID templateId = null;

        if (templateIdStr != null && !templateIdStr.isBlank()) {
            try {
                templateId = UUID.fromString(templateIdStr);
            } catch (IllegalArgumentException e) {
                return ActionResult.failure("Invalid templateId format");
            }

            EmailTemplate_command template = templateRepo.findById(templateId).orElse(null);
            if (template == null) {
                return ActionResult.failure("Email template not found: " + templateId);
            }

            subject = TemplateInterpolator.interpolate(template.getSubject(), input);
            htmlBody = TemplateInterpolator.interpolate(template.getHTMLBody(), input);
            textBody = template.getTextBody() != null
                    ? TemplateInterpolator.interpolate(template.getTextBody(), input) : null;
        } else {
            subject = resolveString(config, "subject", input);
            htmlBody = resolveString(config, "htmlBody", input);
            textBody = resolveString(config, "textBody", input);

            if (subject == null || subject.isBlank()) {
                return ActionResult.failure("Email action requires 'subject' (or a templateId)");
            }
            if (htmlBody == null || htmlBody.isBlank()) {
                return ActionResult.failure("Email action requires 'htmlBody' (or a templateId)");
            }
        }

        UUID emailId = UUID.randomUUID();
        UUID appKeyId = new UUID(0, 0);
        UUID userId = resolveUserId(context.credentials());

        EmailLog log = new EmailLog(emailId, userId, appKeyId, from, to, subject, EmailStatus.PENDING);
        if (templateId != null) {
            log.setTemplateId(templateId);
        }
        emailLogRepo.save(log);

        Map<String, String> emailData = new HashMap<>();
        emailData.put("emailId", emailId.toString());
        emailData.put("userId", userId.toString());
        emailData.put("to", to);
        emailData.put("from", from);
        emailData.put("subject", subject);
        emailData.put("htmlBody", htmlBody);
        if (textBody != null) emailData.put("textBody", textBody);
        if (templateId != null) emailData.put("templateId", templateId.toString());

        Map<String, Object> outboxData = new HashMap<>(emailData);
        outboxRepo.save(new OutboxEvent(
                UUID.randomUUID(),
                RedisStreamConfig.STREAM_EMAIL_QUEUE,
                outboxData
        ));

        logger.info("[email-action] Enqueued email {} to={} from={} subject='{}'",
                emailId, to, from, subject);

        Map<String, Object> output = new HashMap<>();
        output.put("emailId", emailId.toString());
        output.put("to", to);
        output.put("from", from);
        output.put("subject", subject);
        output.put("status", EmailStatus.PENDING.name());
        logger.info("[crescendo-email] Email sent successfully");
        return ActionResult.success(output);
    }

    private String resolveString(Map<String, Object> config, String key, Map<String, Object> input) {
        Object val = config.get(key);
        if (val == null) return null;
        return TemplateInterpolator.interpolate(val.toString(), input);
    }

    private UUID resolveUserId(Map<String, Object> credentials) {
        if (credentials != null && credentials.containsKey("userId")) {
            try {
                return UUID.fromString(credentials.get("userId").toString());
            } catch (IllegalArgumentException ignored) {}
        }
        return new UUID(0, 0);
    }
}
