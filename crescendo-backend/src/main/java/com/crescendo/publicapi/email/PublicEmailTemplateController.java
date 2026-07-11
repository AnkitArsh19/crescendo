package com.crescendo.publicapi.email;

import com.crescendo.emailservice.emailtemplate.EmailTemplateDto;
import com.crescendo.emailservice.emailtemplate.template_command.EmailTemplate_commandService;
import com.crescendo.emailservice.emailtemplate.template_query.EmailTemplate_queryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.crescendo.security.AuthenticatedUser.userId;
import static com.crescendo.publicapi.PublicApiScopes.TEMPLATE_WRITE;
import static com.crescendo.publicapi.PublicApiScopes.TEMPLATE_READ;
import static com.crescendo.publicapi.PublicApiScopes.require;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/templates")
@Tag(name = "Templates", description = "Public API for managing email templates")
public class PublicEmailTemplateController {

    private final EmailTemplate_commandService commandService;
    private final EmailTemplate_queryService queryService;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    public PublicEmailTemplateController(EmailTemplate_commandService commandService,
                                         EmailTemplate_queryService queryService,
                                         org.springframework.data.redis.core.StringRedisTemplate redisTemplate) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.redisTemplate = redisTemplate;
    }

    @PostMapping
    public ResponseEntity<PublicEmailTemplateDto.PublicTemplateResponse> createTemplate(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PublicEmailTemplateDto.CreateTemplateRequest req,
            Authentication auth) {
        require(auth, TEMPLATE_WRITE);
        UUID userId = userId(auth);

        // --- Idempotency enforcement ---
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String redisKey = "public-api:idempotency:template:create:" + userId + ":" + idempotencyKey;
            Boolean claimed = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", java.time.Duration.ofHours(24));
            if (!Boolean.TRUE.equals(claimed)) {
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.CONFLICT,
                        "Duplicate Idempotency-Key: a template was already created with this key.");
            }
        }

        var internalReq = new EmailTemplateDto.CreateTemplateRequest(
                req.name(), req.subject(), req.htmlBody(), req.textBody(), req.variables()
        );
        var resp = commandService.createTemplate(userId, internalReq);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToPublic(resp));
    }

    @GetMapping
    public ResponseEntity<List<PublicEmailTemplateDto.PublicTemplateResponse>> listTemplates(Authentication auth) {
        require(auth, TEMPLATE_READ);
        List<EmailTemplateDto.TemplateResponse> templates = queryService.listTemplates(userId(auth));
        return ResponseEntity.ok(templates.stream().map(this::mapToPublic).toList());
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<PublicEmailTemplateDto.PublicTemplateResponse> getTemplate(
            @PathVariable UUID templateId,
            Authentication auth) {
        require(auth, TEMPLATE_READ);
        return ResponseEntity.ok(mapToPublic(queryService.getTemplate(userId(auth), templateId)));
    }

    @PatchMapping("/{templateId}")
    public ResponseEntity<PublicEmailTemplateDto.PublicTemplateResponse> updateTemplate(
            @PathVariable UUID templateId,
            @Valid @RequestBody PublicEmailTemplateDto.UpdateTemplateRequest req,
            Authentication auth) {
        require(auth, TEMPLATE_WRITE);
        var internalReq = new EmailTemplateDto.UpdateTemplateRequest(
                req.name(), req.subject(), req.htmlBody(), req.textBody(), req.variables()
        );
        var resp = commandService.updateTemplate(userId(auth), templateId, internalReq);
        return ResponseEntity.ok(mapToPublic(resp));
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable UUID templateId,
            Authentication auth) {
        require(auth, TEMPLATE_WRITE);
        commandService.deleteTemplate(userId(auth), templateId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{templateId}/publish")
    public ResponseEntity<PublicEmailTemplateDto.PublicTemplateResponse> publishTemplate(
            @PathVariable UUID templateId,
            Authentication auth) {
        require(auth, TEMPLATE_WRITE);
        var resp = commandService.publishTemplate(userId(auth), templateId);
        return ResponseEntity.ok(mapToPublic(resp));
    }

    @PostMapping("/{templateId}/test")
    public ResponseEntity<PublicEmailTemplateDto.PublicTestSendResponse> testSend(
            @PathVariable UUID templateId,
            @Valid @RequestBody PublicEmailTemplateDto.TestSendRequest req,
            Authentication auth) {
        require(auth, TEMPLATE_WRITE);
        var internalReq = new EmailTemplateDto.TestSendRequest(req.toAddress(), req.variables());
        var resp = commandService.testSend(userId(auth), templateId, internalReq);
        return ResponseEntity.ok(new PublicEmailTemplateDto.PublicTestSendResponse(
                resp.emailId(), "PENDING"
        ));
    }

    @PostMapping("/clone-from-broadcast/{broadcastId}")
    public ResponseEntity<PublicEmailTemplateDto.PublicTemplateResponse> cloneFromBroadcast(
            @PathVariable UUID broadcastId,
            Authentication auth) {
        require(auth, TEMPLATE_WRITE);
        var resp = commandService.cloneFromBroadcast(userId(auth), broadcastId);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToPublic(resp));
    }

    private PublicEmailTemplateDto.PublicTemplateResponse mapToPublic(EmailTemplateDto.TemplateResponse resp) {
        return new PublicEmailTemplateDto.PublicTemplateResponse(
                resp.id().toString(),
                resp.name(),
                resp.subject(),
                resp.htmlBody(),
                resp.textBody(),
                resp.status(),
                resp.variables(),
                resp.hasPublishedSnapshot(),
                resp.publishedAt(),
                resp.createdAt(),
                resp.updatedAt()
        );
    }
}
