package com.crescendo.emailservice.emailtemplate;

import com.crescendo.emailservice.emailtemplate.template_command.EmailTemplate_commandService;
import com.crescendo.emailservice.emailtemplate.template_query.EmailTemplate_queryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.crescendo.security.AuthenticatedUser.userId;

import java.util.List;
import java.util.UUID;

/**
 * Email template CRUD endpoints under /settings/templates.
 *
 *   POST   /settings/templates           — create template
 *   GET    /settings/templates           — list templates
 *   GET    /settings/templates/{id}      — get template
 *   PATCH  /settings/templates/{id}      — update template
 *   DELETE /settings/templates/{id}      — delete template
 */
@RestController
@RequestMapping("/settings/templates")
public class EmailTemplateController {

    private final EmailTemplate_commandService commandService;
    private final EmailTemplate_queryService queryService;

    public EmailTemplateController(EmailTemplate_commandService commandService,
                                   EmailTemplate_queryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<EmailTemplateDto.TemplateResponse> createTemplate(
            @Valid @RequestBody EmailTemplateDto.CreateTemplateRequest req,
            Authentication auth) {
        var resp = commandService.createTemplate(userId(auth), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping
    public ResponseEntity<List<EmailTemplateDto.TemplateResponse>> listTemplates(Authentication auth) {
        return ResponseEntity.ok(queryService.listTemplates(userId(auth)));
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<EmailTemplateDto.TemplateResponse> getTemplate(
            @PathVariable UUID templateId,
            Authentication auth) {
        return ResponseEntity.ok(queryService.getTemplate(userId(auth), templateId));
    }

    @PatchMapping("/{templateId}")
    public ResponseEntity<EmailTemplateDto.TemplateResponse> updateTemplate(
            @PathVariable UUID templateId,
            @Valid @RequestBody EmailTemplateDto.UpdateTemplateRequest req,
            Authentication auth) {
        var resp = commandService.updateTemplate(userId(auth), templateId, req);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable UUID templateId,
            Authentication auth) {
        commandService.deleteTemplate(userId(auth), templateId);
        return ResponseEntity.noContent().build();
    }

}
