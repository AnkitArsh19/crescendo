package com.crescendo.emailservice.emailtemplate.template_query;

import com.crescendo.config.DataSeeder;
import com.crescendo.emailservice.emailtemplate.EmailTemplateDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Read-side service for email templates.
 */
@Service
@Transactional(readOnly = true)
public class EmailTemplate_queryService {

    private final EmailTemplate_queryRepository queryRepo;

    public EmailTemplate_queryService(EmailTemplate_queryRepository queryRepo) {
        this.queryRepo = queryRepo;
    }

    /**
     * Returns the user's own templates plus system-level starter templates.
     */
    public List<EmailTemplateDto.TemplateResponse> listTemplates(UUID userId) {
        List<EmailTemplateDto.TemplateResponse> result = new ArrayList<>();
        result.addAll(queryRepo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList());
        if (!DataSeeder.SYSTEM_USER_ID.equals(userId)) {
            result.addAll(queryRepo.findByUserIdOrderByCreatedAtDesc(DataSeeder.SYSTEM_USER_ID)
                    .stream().map(this::toResponse).toList());
        }
        return result;
    }

    public EmailTemplateDto.TemplateResponse getTemplate(UUID userId, UUID templateId) {
        EmailTemplate_query template = queryRepo.findByIdAndUserId(templateId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));
        return toResponse(template);
    }

    private EmailTemplateDto.TemplateResponse toResponse(EmailTemplate_query t) {
        return new EmailTemplateDto.TemplateResponse(
                t.getId(), t.getName(), t.getSubject(),
                t.getHTMLBody(), t.getTextBody(),
                t.getCreatedAt(), t.getUpdatedAt());
    }
}
