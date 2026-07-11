package com.crescendo.apps.crescendomail;

import com.crescendo.emailservice.email_log.EmailLog;
import com.crescendo.emailservice.email_log.EmailLogRepository;
import com.crescendo.emailservice.emailtemplate.template_command.EmailTemplate_command;
import com.crescendo.emailservice.emailtemplate.template_command.EmailTemplate_commandRepository;
import com.crescendo.enums.EmailStatus;
import com.crescendo.enums.EmailType;
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
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Send, SendTemplated, and SendBatch action handlers for crescendomail.
 *
 * <p>All string config fields support {@code {{variable}}} interpolation from input data.
 *
 * <p>Action keys:
 * <ul>
 *   <li>{@code crescendomail:send}          — raw HTML/text email
 *   <li>{@code crescendomail:send-templated} — by published template ID + variable map
 *   <li>{@code crescendomail:send-batch}     — up to 100 recipients, shared or per-recipient vars
 * </ul>
 */
public class CrescendoMailSendHandlers {

    // ─────────────────────────────────────────────────────────────────────────
    // send
    // ─────────────────────────────────────────────────────────────────────────

    @Component
    @ActionMapping(appKey = "crescendomail", actionKey = "send")
    public static class SendHandler implements ActionHandler {

        private static final Logger log = LoggerFactory.getLogger(SendHandler.class);

        private final EmailLogRepository emailLogRepo;
        private final OutboxEventRepository outboxRepo;

        public SendHandler(EmailLogRepository emailLogRepo, OutboxEventRepository outboxRepo) {
            this.emailLogRepo = emailLogRepo;
            this.outboxRepo = outboxRepo;
        }

        @Override
        public ActionResult execute(ActionContext ctx) {
            Map<String, Object> cfg = ctx.configuration();
            Map<String, Object> input = ctx.input();

            String to      = interpolate(cfg, "to",      input);
            String from    = interpolate(cfg, "from",    input);
            String subject = interpolate(cfg, "subject", input);
            String html    = interpolate(cfg, "htmlBody", input);
            String text    = interpolate(cfg, "textBody", input);

            if (blank(to))      return ActionResult.failure("crescendomail:send requires 'to'");
            if (blank(from))    return ActionResult.failure("crescendomail:send requires 'from'");
            if (blank(subject)) return ActionResult.failure("crescendomail:send requires 'subject'");
            if (blank(html))    return ActionResult.failure("crescendomail:send requires 'htmlBody'");

            EmailType emailType = resolveEmailType(cfg, input);
            if (emailType == null) return ActionResult.failure("Invalid emailType — must be TRANSACTIONAL or MARKETING");

            UUID emailId = UUID.randomUUID();
            UUID userId  = resolveUserId(ctx);
            UUID appKeyId = new UUID(0, 0);

            EmailLog emailLog = new EmailLog(emailId, userId, appKeyId, from, to, subject, EmailStatus.PENDING, emailType);
            // Auto-tag with workflow context when triggered from the execution engine
            if (ctx.workflowRunId() != null) emailLog.addTag("workflowRunId", ctx.workflowRunId().toString());
            if (ctx.stepId()        != null) emailLog.addTag("stepId",        ctx.stepId().toString());
            emailLogRepo.save(emailLog);

            Map<String, Object> payload = buildQueuePayload(emailId, userId, to, from, subject, html, text, null);
            outboxRepo.save(new OutboxEvent(UUID.randomUUID(), RedisStreamConfig.STREAM_EMAIL_QUEUE, payload));

            log.info("[crescendomail:send] Enqueued {} to={} subject='{}'", emailId, to, subject);
            return ActionResult.success(buildOutput(emailId, to, from, subject));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // send-templated
    // ─────────────────────────────────────────────────────────────────────────

    @Component
    @ActionMapping(appKey = "crescendomail", actionKey = "send-templated")
    public static class SendTemplatedHandler implements ActionHandler {

        private static final Logger log = LoggerFactory.getLogger(SendTemplatedHandler.class);

        private final EmailLogRepository emailLogRepo;
        private final EmailTemplate_commandRepository templateRepo;
        private final OutboxEventRepository outboxRepo;

        public SendTemplatedHandler(EmailLogRepository emailLogRepo,
                                    EmailTemplate_commandRepository templateRepo,
                                    OutboxEventRepository outboxRepo) {
            this.emailLogRepo = emailLogRepo;
            this.templateRepo = templateRepo;
            this.outboxRepo = outboxRepo;
        }

        @Override
        public ActionResult execute(ActionContext ctx) {
            Map<String, Object> cfg = ctx.configuration();
            Map<String, Object> input = ctx.input();

            String to   = interpolate(cfg, "to",   input);
            String from = interpolate(cfg, "from", input);
            String templateIdStr = ctx.getString("templateId");

            if (blank(to))            return ActionResult.failure("crescendomail:send-templated requires 'to'");
            if (blank(from))          return ActionResult.failure("crescendomail:send-templated requires 'from'");
            if (blank(templateIdStr)) return ActionResult.failure("crescendomail:send-templated requires 'templateId'");

            UUID templateId;
            try {
                templateId = UUID.fromString(templateIdStr);
            } catch (IllegalArgumentException e) {
                return ActionResult.failure("Invalid templateId format");
            }

            EmailTemplate_command template = templateRepo.findById(templateId).orElse(null);
            if (template == null) return ActionResult.failure("Template not found: " + templateId);

            // Merge step config variables into input for interpolation
            Map<String, Object> vars = new HashMap<>(input);
            Object configVars = cfg.get("variables");
            if (configVars instanceof Map<?, ?> m) {
                m.forEach((k, v) -> vars.put(String.valueOf(k), v));
            }

            String subject = TemplateInterpolator.interpolate(template.getSubject(), vars);
            String html    = TemplateInterpolator.interpolate(template.getHTMLBody(), vars);
            String text    = template.getTextBody() != null
                    ? TemplateInterpolator.interpolate(template.getTextBody(), vars) : null;

            EmailType emailType = resolveEmailType(cfg, input);
            if (emailType == null) emailType = EmailType.TRANSACTIONAL;

            UUID emailId  = UUID.randomUUID();
            UUID userId   = resolveUserId(ctx);
            UUID appKeyId = new UUID(0, 0);

            EmailLog emailLog = new EmailLog(emailId, userId, appKeyId, from, to, subject, EmailStatus.PENDING, emailType);
            emailLog.setTemplateId(templateId);
            if (ctx.workflowRunId() != null) emailLog.addTag("workflowRunId", ctx.workflowRunId().toString());
            if (ctx.stepId()        != null) emailLog.addTag("stepId",        ctx.stepId().toString());
            emailLogRepo.save(emailLog);

            Map<String, Object> payload = buildQueuePayload(emailId, userId, to, from, subject, html, text, templateId);
            outboxRepo.save(new OutboxEvent(UUID.randomUUID(), RedisStreamConfig.STREAM_EMAIL_QUEUE, payload));

            log.info("[crescendomail:send-templated] Enqueued {} template={} to={}", emailId, templateId, to);
            return ActionResult.success(buildOutput(emailId, to, from, subject));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // send-batch
    // ─────────────────────────────────────────────────────────────────────────

    @Component
    @ActionMapping(appKey = "crescendomail", actionKey = "send-batch")
    public static class SendBatchHandler implements ActionHandler {

        private static final Logger log = LoggerFactory.getLogger(SendBatchHandler.class);
        private static final int MAX_BATCH = 100;

        private final EmailLogRepository emailLogRepo;
        private final OutboxEventRepository outboxRepo;

        public SendBatchHandler(EmailLogRepository emailLogRepo, OutboxEventRepository outboxRepo) {
            this.emailLogRepo = emailLogRepo;
            this.outboxRepo = outboxRepo;
        }

        @Override
        public ActionResult execute(ActionContext ctx) {
            Map<String, Object> cfg = ctx.configuration();
            Map<String, Object> input = ctx.input();

            Object recipientsRaw = cfg.get("recipients");
            if (!(recipientsRaw instanceof List<?> recipientList) || recipientList.isEmpty()) {
                return ActionResult.failure("crescendomail:send-batch requires 'recipients' list");
            }
            if (recipientList.size() > MAX_BATCH) {
                return ActionResult.failure("crescendomail:send-batch supports at most " + MAX_BATCH + " recipients");
            }

            String from    = interpolate(cfg, "from",    input);
            String subject = interpolate(cfg, "subject", input);
            String html    = interpolate(cfg, "htmlBody", input);
            if (blank(from))    return ActionResult.failure("crescendomail:send-batch requires 'from'");
            if (blank(subject)) return ActionResult.failure("crescendomail:send-batch requires 'subject'");
            if (blank(html))    return ActionResult.failure("crescendomail:send-batch requires 'htmlBody'");

            EmailType emailType = resolveEmailType(cfg, input);
            if (emailType == null) emailType = EmailType.TRANSACTIONAL;

            UUID userId   = resolveUserId(ctx);
            UUID appKeyId = new UUID(0, 0);
            int queued = 0;

            for (Object recipientRaw : recipientList) {
                String to;
                Map<String, Object> perRecipientVars = new HashMap<>(input);

                if (recipientRaw instanceof Map<?, ?> recipientMap) {
                    to = String.valueOf(recipientMap.get("to"));
                    recipientMap.forEach((k, v) -> perRecipientVars.put(String.valueOf(k), v));
                } else {
                    to = String.valueOf(recipientRaw);
                }

                if (blank(to)) continue;

                String resolvedSubject = TemplateInterpolator.interpolate(subject, perRecipientVars);
                String resolvedHtml    = TemplateInterpolator.interpolate(html, perRecipientVars);

                UUID emailId = UUID.randomUUID();
                EmailLog emailLog = new EmailLog(emailId, userId, appKeyId, from, to, resolvedSubject, EmailStatus.PENDING, emailType);
                emailLogRepo.save(emailLog);

                Map<String, Object> payload = buildQueuePayload(emailId, userId, to, from, resolvedSubject, resolvedHtml, null, null);
                outboxRepo.save(new OutboxEvent(UUID.randomUUID(), RedisStreamConfig.STREAM_EMAIL_QUEUE, payload));
                queued++;
            }

            log.info("[crescendomail:send-batch] Queued {} of {} recipients", queued, recipientList.size());
            return ActionResult.success(Map.of("queued", queued, "total", recipientList.size()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shared helpers
    // ─────────────────────────────────────────────────────────────────────────

    static String interpolate(Map<String, Object> cfg, String key, Map<String, Object> input) {
        Object val = cfg.get(key);
        if (val == null) return null;
        return TemplateInterpolator.interpolate(val.toString(), input);
    }

    static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    static EmailType resolveEmailType(Map<String, Object> cfg, Map<String, Object> input) {
        Object raw = cfg.get("emailType");
        if (raw == null) return EmailType.TRANSACTIONAL;
        try {
            return EmailType.valueOf(raw.toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static UUID resolveUserId(ActionContext ctx) {
        if (ctx.userId() != null) return ctx.userId();
        Map<String, Object> creds = ctx.credentials();
        if (creds != null && creds.containsKey("userId")) {
            try { return UUID.fromString(creds.get("userId").toString()); } catch (IllegalArgumentException ignored) {}
        }
        return new UUID(0, 0);
    }

    static Map<String, Object> buildQueuePayload(UUID emailId, UUID userId, String to, String from,
                                                   String subject, String html, String text, UUID templateId) {
        Map<String, Object> p = new HashMap<>();
        p.put("emailId",  emailId.toString());
        p.put("userId",   userId.toString());
        p.put("to",       to);
        p.put("from",     from);
        p.put("subject",  subject);
        p.put("htmlBody", html);
        if (text != null) p.put("textBody", text);
        if (templateId != null) p.put("templateId", templateId.toString());
        return p;
    }

    static Map<String, Object> buildOutput(UUID emailId, String to, String from, String subject) {
        Map<String, Object> out = new HashMap<>();
        out.put("emailId", emailId.toString());
        out.put("to",      to);
        out.put("from",    from);
        out.put("subject", subject);
        out.put("status",  EmailStatus.PENDING.name());
        return out;
    }
}
