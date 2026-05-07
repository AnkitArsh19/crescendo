package com.crescendo.emailservice.emailtemplate.template_command;

import com.crescendo.emailservice.emailtemplate.EmailTemplateDto;
import com.crescendo.emailservice.emailtemplate.template_query.EmailTemplate_query;
import com.crescendo.emailservice.emailtemplate.template_query.EmailTemplate_queryRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Write-side service for email template management.
 *
 * Every mutation:
 *   1. Writes to the command database
 *   2. Synchronously projects to the query database
 */
@Service
@Transactional
public class EmailTemplate_commandService {

    private static final int MAX_TEMPLATES_PER_USER = 50;

    private final EmailTemplate_commandRepository commandRepo;
    private final EmailTemplate_queryRepository queryRepo;

    public EmailTemplate_commandService(EmailTemplate_commandRepository commandRepo,
                                        EmailTemplate_queryRepository queryRepo) {
        this.commandRepo = commandRepo;
        this.queryRepo = queryRepo;
    }

    public EmailTemplateDto.TemplateResponse createTemplate(UUID userId,
                                                             EmailTemplateDto.CreateTemplateRequest req) {
        long count = commandRepo.findByUserIdOrderByCreatedAtDesc(userId).size();
        if (count >= MAX_TEMPLATES_PER_USER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Maximum templates reached (" + MAX_TEMPLATES_PER_USER + ")");
        }

        UUID templateId = UUID.randomUUID();
        EmailTemplate_command template = new EmailTemplate_command(
                templateId, userId, req.name(), req.subject(), req.htmlBody(), req.textBody());
        commandRepo.save(template);

        // Sync to query database
        queryRepo.save(new EmailTemplate_query(
                templateId, userId, req.name(), req.subject(), req.htmlBody(), req.textBody()));

        return toResponse(template);
    }

    public EmailTemplateDto.TemplateResponse updateTemplate(UUID userId, UUID templateId,
                                                             EmailTemplateDto.UpdateTemplateRequest req) {
        EmailTemplate_command template = commandRepo.findByIdAndUserId(templateId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));

        if (req.name() == null && req.subject() == null && req.htmlBody() == null && req.textBody() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one field must be provided");
        }

        if (req.name() != null) template.setName(req.name());
        if (req.subject() != null) template.setSubject(req.subject());
        if (req.htmlBody() != null) template.setHTMLBody(req.htmlBody());
        if (req.textBody() != null) template.setTextBody(req.textBody());

        // Sync to query side
        queryRepo.findByIdAndUserId(templateId, userId).ifPresent(q -> {
            if (req.name() != null) q.setName(req.name());
            if (req.subject() != null) q.setSubject(req.subject());
            if (req.htmlBody() != null) q.setHTMLBody(req.htmlBody());
            if (req.textBody() != null) q.setTextBody(req.textBody());
        });

        return toResponse(template);
    }

    public void deleteTemplate(UUID userId, UUID templateId) {
        EmailTemplate_command template = commandRepo.findByIdAndUserId(templateId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));
        commandRepo.delete(template);
        queryRepo.deleteById(templateId);
    }

    private EmailTemplateDto.TemplateResponse toResponse(EmailTemplate_command t) {
        return new EmailTemplateDto.TemplateResponse(
                t.getId(), t.getName(), t.getSubject(),
                t.getHTMLBody(), t.getTextBody(),
                t.getCreatedAt(), t.getUpdatedAt());
    }
}
