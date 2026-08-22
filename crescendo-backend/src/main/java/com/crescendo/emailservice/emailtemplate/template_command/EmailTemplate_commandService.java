package com.crescendo.emailservice.emailtemplate.template_command;

import com.crescendo.emailservice.broadcast.Broadcast;
import com.crescendo.emailservice.broadcast.BroadcastRepository;
import com.crescendo.emailservice.email_log.EmailLog;
import com.crescendo.emailservice.email_log.EmailLogRepository;
import com.crescendo.emailservice.emailtemplate.EmailTemplateDto;
import com.crescendo.emailservice.emailtemplate.EmailTemplateHtmlSanitizer;
import com.crescendo.emailservice.emailtemplate.TemplateVariableValidator;
import com.crescendo.emailservice.emailtemplate.template_query.EmailTemplate_query;
import com.crescendo.emailservice.emailtemplate.template_query.EmailTemplate_queryRepository;
import com.crescendo.enums.EmailStatus;
import com.crescendo.enums.EmailType;
import com.crescendo.logbook.outbox.OutboxEvent;
import com.crescendo.logbook.outbox.OutboxEventRepository;
import com.crescendo.config.RedisStreamConfig;
import com.crescendo.shared.util.TemplateInterpolator;
import com.crescendo.user.user_command.User_commandRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Write-side service for email template management.
 *
 * <p>Every mutation writes to the command database and synchronously projects to the query database.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>createTemplate  — DRAFT
 *   <li>updateTemplate  — still DRAFT (resets snapshot invalidation note to user)
 *   <li>publishTemplate — validates variables, freezes snapshot, status → PUBLISHED
 *   <li>testSend        — sends to caller's address, tagged isTest=true, not production-counted
 * </ol>
 */
@Service
@Transactional
public class EmailTemplate_commandService {

    private static final int MAX_TEMPLATES_PER_USER = 50;

    private final EmailTemplate_commandRepository commandRepo;
    private final EmailTemplate_queryRepository   queryRepo;
    private final TemplateVariableValidator       validator;
    private final BroadcastRepository             broadcastRepo;
    private final EmailLogRepository              emailLogRepo;
    private final OutboxEventRepository           outboxRepo;
    private final User_commandRepository          userRepo;
    private final EmailTemplateHtmlSanitizer       htmlSanitizer;

    public EmailTemplate_commandService(
            EmailTemplate_commandRepository commandRepo,
            EmailTemplate_queryRepository queryRepo,
            TemplateVariableValidator validator,
            BroadcastRepository broadcastRepo,
            EmailLogRepository emailLogRepo,
                                     OutboxEventRepository outboxRepo,
                                     User_commandRepository userRepo,
                                     EmailTemplateHtmlSanitizer htmlSanitizer) {
        this.commandRepo   = commandRepo;
        this.queryRepo     = queryRepo;
        this.validator     = validator;
        this.broadcastRepo = broadcastRepo;
        this.emailLogRepo  = emailLogRepo;
        this.outboxRepo    = outboxRepo;
        this.userRepo      = userRepo;
        this.htmlSanitizer = htmlSanitizer;
    }

    // ── Create ───────────────────────────────────────────────────────────────

    public EmailTemplateDto.TemplateResponse createTemplate(UUID userId,
                                                             EmailTemplateDto.CreateTemplateRequest req) {
        long count = commandRepo.findByUserIdOrderByCreatedAtDesc(userId).size();
        if (count >= MAX_TEMPLATES_PER_USER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Maximum templates reached (" + MAX_TEMPLATES_PER_USER + ")");
        }

        UUID templateId = UUID.randomUUID();
        EmailTemplate_command template = new EmailTemplate_command(
                templateId, userId, req.name(), req.subject(), htmlSanitizer.sanitize(req.htmlBody()), req.textBody());
        if (req.variables() != null) template.setVariables(req.variables());
        applyEditorMetadata(template, req.fromAddress(), req.replyTo(), req.previewText(), req.editorDocument());
        commandRepo.save(template);

        EmailTemplate_query queryModel = new EmailTemplate_query(
                templateId, userId, req.name(), req.subject(), htmlSanitizer.sanitize(req.htmlBody()), req.textBody());
        if (req.variables() != null) queryModel.setVariables(new ArrayList<>(req.variables()));
        applyEditorMetadata(queryModel, req.fromAddress(), req.replyTo(), req.previewText(), req.editorDocument());
        queryRepo.save(queryModel);

        return toResponse(template);
    }

    // ── Update ───────────────────────────────────────────────────────────────

    public EmailTemplateDto.TemplateResponse updateTemplate(UUID userId, UUID templateId,
                                                             EmailTemplateDto.UpdateTemplateRequest req) {
        EmailTemplate_command template = commandRepo.findByIdAndUserId(templateId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));

        if (req.name() == null && req.subject() == null && req.htmlBody() == null
                && req.textBody() == null && req.variables() == null && req.fromAddress() == null
                && req.replyTo() == null && req.previewText() == null && req.editorDocument() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one field must be provided");
        }

        if (req.name()      != null) template.setName(req.name());
        if (req.subject()   != null) template.setSubject(req.subject());
        if (req.htmlBody()  != null) template.setHTMLBody(htmlSanitizer.sanitize(req.htmlBody()));
        if (req.textBody()  != null) template.setTextBody(req.textBody());
        if (req.variables() != null) template.setVariables(req.variables());
        applyEditorMetadata(template, req.fromAddress(), req.replyTo(), req.previewText(), req.editorDocument());

        queryRepo.findByIdAndUserId(templateId, userId).ifPresent(q -> {
            if (req.name()      != null) q.setName(req.name());
            if (req.subject()   != null) q.setSubject(req.subject());
            if (req.htmlBody()  != null) q.setHTMLBody(htmlSanitizer.sanitize(req.htmlBody()));
            if (req.textBody()  != null) q.setTextBody(req.textBody());
            if (req.variables() != null) q.setVariables(new ArrayList<>(req.variables()));
            applyEditorMetadata(q, req.fromAddress(), req.replyTo(), req.previewText(), req.editorDocument());
            queryRepo.save(q);
        });

        return toResponse(template);
    }

    // ── Publish ──────────────────────────────────────────────────────────────

    /**
     * Publishes a template:
     * 1. Validates that every {{VAR}} in subject/body is declared or reserved.
     * 2. Freezes a {@link EmailTemplate_command.PublishedSnapshot} so later draft edits
     *    don't retroactively affect what was sent.
     * 3. Flips status to PUBLISHED.
     */
    public EmailTemplateDto.TemplateResponse publishTemplate(UUID userId, UUID templateId) {
        EmailTemplate_command template = commandRepo.findByIdAndUserId(templateId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));

        validator.validateForPublish(template);

        EmailTemplate_command.PublishedSnapshot snapshot = new EmailTemplate_command.PublishedSnapshot(
                template.getSubject(),
                template.getHTMLBody(),
                template.getTextBody(),
                new ArrayList<>(template.getVariables()),
                Instant.now()
        );
        template.setPublishedVersionSnapshot(snapshot);
        template.setStatus(EmailTemplate_command.TemplateStatus.PUBLISHED);
        commandRepo.save(template);

        queryRepo.findByIdAndUserId(templateId, userId).ifPresent(q -> {
            q.setStatus(EmailTemplate_command.TemplateStatus.PUBLISHED);
            q.setPublishedVersionSnapshot(snapshot);
            queryRepo.save(q);
        });

        return toResponse(template);
    }

    // ── Test Send ─────────────────────────────────────────────────────────────

    /**
     * Sends a test email to the specified address using the current draft content.
     * Tagged isTest=true in EmailLog; does NOT require template to be published.
     * Does NOT count against production metrics.
     */
    public EmailTemplateDto.TestSendResponse testSend(UUID userId, UUID templateId,
                                                       EmailTemplateDto.TestSendRequest req) {
        EmailTemplate_command template = commandRepo.findByIdAndUserId(templateId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));

        String verifiedRecipient = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .getEmailId();
        if (!verifiedRecipient.equalsIgnoreCase(req.toAddress())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Test emails may only be sent to your verified account address");
        }

        Map<String, Object> vars = new HashMap<>();
        for (var v : template.getVariables()) {
            if (v.fallbackValue() != null) vars.put(v.name(), v.fallbackValue());
        }
        if (req.variables() != null) vars.putAll(req.variables());
        if (!vars.containsKey("FIRST_NAME")) vars.put("FIRST_NAME", "Jane");
        if (!vars.containsKey("LAST_NAME")) vars.put("LAST_NAME", "Doe");
        if (!vars.containsKey("EMAIL")) vars.put("EMAIL", req.toAddress());
        if (!vars.containsKey("COMPANY_NAME")) vars.put("COMPANY_NAME", "Acme Corp");
        if (!vars.containsKey("CRESCENDO_UNSUBSCRIBE_URL")) vars.put("CRESCENDO_UNSUBSCRIBE_URL", "#unsubscribe");
        if (!vars.containsKey("UNSUBSCRIBE_URL")) vars.put("UNSUBSCRIBE_URL", "#unsubscribe");
        if (!vars.containsKey("CURRENT_YEAR")) vars.put("CURRENT_YEAR", String.valueOf(java.time.Year.now().getValue()));

        String subject  = TemplateInterpolator.interpolate(template.getSubject(),  vars);
        String htmlBody = TemplateInterpolator.interpolate(template.getHTMLBody(), vars);
        String textBody = template.getTextBody() == null ? null
                : TemplateInterpolator.interpolate(template.getTextBody(), vars);

        UUID emailId  = UUID.randomUUID();
        UUID appKeyId = new UUID(0, 0);
        String from = template.getFromAddress() == null || template.getFromAddress().isBlank()
                ? "test@crescendo.run" : template.getFromAddress();

        EmailLog log = new EmailLog(emailId, userId, appKeyId, from, req.toAddress(),
                subject, EmailStatus.PENDING, EmailType.TRANSACTIONAL);
        log.setTemplateId(templateId);
        log.addTag("isTest",     "true");
        log.addTag("templateId", templateId.toString());
        emailLogRepo.save(log);

        Map<String, Object> payload = new HashMap<>();
        payload.put("emailId",  emailId.toString());
        payload.put("userId",   userId.toString());
        payload.put("to",       req.toAddress());
        payload.put("from",     from);
        payload.put("subject",  subject);
        payload.put("htmlBody", htmlBody);
        if (textBody != null) payload.put("textBody", textBody);
        payload.put("templateId", templateId.toString());

        outboxRepo.save(new OutboxEvent(UUID.randomUUID(), RedisStreamConfig.STREAM_EMAIL_QUEUE, payload));

        return new EmailTemplateDto.TestSendResponse(emailId.toString(), req.toAddress(), subject, true);
    }

    // ── Clone Broadcast as Template ───────────────────────────────────────────

    /**
     * Clones a broadcast's HTML content into a new draft template.
     * Useful for re-using a one-off broadcast as a reusable template.
     */
    public EmailTemplateDto.TemplateResponse cloneFromBroadcast(UUID userId, UUID broadcastId) {
        Broadcast broadcast = broadcastRepo.findByIdAndUserId(broadcastId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Broadcast not found"));

        // Look up the broadcast's template content
        EmailTemplate_command source = commandRepo.findByIdAndUserId(broadcast.getTemplateId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Broadcast template not found: " + broadcast.getTemplateId()));

        UUID newId = UUID.randomUUID();
        EmailTemplate_command clone = new EmailTemplate_command(
                newId, userId,
                source.getName() + " (from broadcast)",
                source.getSubject(),
                source.getHTMLBody(),
                source.getTextBody()
        );
        clone.setVariables(new ArrayList<>(source.getVariables()));
        applyEditorMetadata(clone, source.getFromAddress(), source.getReplyTo(), source.getPreviewText(), source.getEditorDocument());
        commandRepo.save(clone);

        EmailTemplate_query queryModel = new EmailTemplate_query(
                newId, userId, clone.getName(), clone.getSubject(),
                clone.getHTMLBody(), clone.getTextBody());
        queryModel.setVariables(new ArrayList<>(source.getVariables()));
        applyEditorMetadata(queryModel, source.getFromAddress(), source.getReplyTo(), source.getPreviewText(), source.getEditorDocument());
        queryRepo.save(queryModel);

        return toResponse(clone);
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    public void deleteTemplate(UUID userId, UUID templateId) {
        EmailTemplate_command template = commandRepo.findByIdAndUserId(templateId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));
        commandRepo.delete(template);
        queryRepo.deleteById(templateId);
    }

    // ── toResponse ────────────────────────────────────────────────────────────

    private EmailTemplateDto.TemplateResponse toResponse(EmailTemplate_command t) {
        EmailTemplate_command.PublishedSnapshot snap = t.getPublishedVersionSnapshot();
        return new EmailTemplateDto.TemplateResponse(
                t.getId(),
                t.getName(),
                t.getSubject(),
                t.getHTMLBody(),
                t.getTextBody(),
                t.getFromAddress(),
                t.getReplyTo(),
                t.getPreviewText(),
                t.getEditorDocument(),
                t.getStatus().name(),
                t.getVariables() != null ? t.getVariables() : List.of(),
                snap != null,
                snap != null ? snap.publishedAt() : null,
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }

    private void applyEditorMetadata(EmailTemplate_command template, String fromAddress, String replyTo,
                                     String previewText, String editorDocument) {
        if (fromAddress != null) template.setFromAddress(fromAddress);
        if (replyTo != null) template.setReplyTo(replyTo);
        if (previewText != null) template.setPreviewText(previewText);
        if (editorDocument != null) template.setEditorDocument(editorDocument);
    }

    private void applyEditorMetadata(EmailTemplate_query template, String fromAddress, String replyTo,
                                     String previewText, String editorDocument) {
        if (fromAddress != null) template.setFromAddress(fromAddress);
        if (replyTo != null) template.setReplyTo(replyTo);
        if (previewText != null) template.setPreviewText(previewText);
        if (editorDocument != null) template.setEditorDocument(editorDocument);
    }
}

