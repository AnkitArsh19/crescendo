package com.crescendo.emailservice.email_send;

import com.crescendo.security.ApiKeyAuthenticationFilter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.crescendo.security.AuthenticatedUser.userId;
import static com.crescendo.publicapi.PublicApiScopes.EMAIL_SEND;
import static com.crescendo.publicapi.PublicApiScopes.require;

import java.util.UUID;

/**
 * Public email sending API — authenticated via API key (Bearer re_...).
 * Mirrors Resend's API design for familiarity:
 *
 * POST /api/v1/emails — send an email (returns immediately, delivery is async)
 *
 * This endpoint is accessible to both JWT-authenticated dashboard users
 * and API-key-authenticated external services.
 */
@RestController
@RequestMapping("/api/v1/emails")
@Tag(name = "Emails", description = "Public API for sending and tracking emails")
public class SendEmailController {

    private final EmailSendService emailSendService;

    public SendEmailController(EmailSendService emailSendService) {
        this.emailSendService = emailSendService;
    }

    @PostMapping
    public ResponseEntity<EmailSendDto.SendEmailResponse> sendEmail(
            @Valid @RequestBody EmailSendDto.SendEmailRequest req,
            Authentication auth,
            HttpServletRequest request) {
        UUID apiKeyId = resolveApiKeyId(request);
        if (apiKeyId.getMostSignificantBits() != 0 || apiKeyId.getLeastSignificantBits() != 0) {
            require(auth, EMAIL_SEND);
        }
        UUID userId = userId(auth);

        var resp = emailSendService.sendEmail(userId, apiKeyId, req);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(resp);
    }

    private UUID resolveApiKeyId(HttpServletRequest request) {
        Object attr = request.getAttribute(ApiKeyAuthenticationFilter.API_KEY_ID_ATTRIBUTE);
        if (attr instanceof UUID uuid)
            return uuid;
        return new UUID(0, 0);
    }

    @PostMapping("/templated")
    public ResponseEntity<EmailSendDto.SendEmailResponse> sendTemplated(
            @Valid @RequestBody EmailSendDto.SendTemplatedRequest req,
            Authentication auth,
            HttpServletRequest request) {
        UUID apiKeyId = resolveApiKeyId(request);
        if (apiKeyId.getMostSignificantBits() != 0 || apiKeyId.getLeastSignificantBits() != 0) {
            require(auth, EMAIL_SEND);
        }
        UUID userId = userId(auth);

        var resp = emailSendService.sendTemplated(userId, apiKeyId, req);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(resp);
    }

    @PostMapping("/batch")
    public ResponseEntity<EmailSendDto.SendBatchResponse> sendBatch(
            @Valid @RequestBody EmailSendDto.SendBatchRequest req,
            Authentication auth,
            HttpServletRequest request) {
        UUID apiKeyId = resolveApiKeyId(request);
        if (apiKeyId.getMostSignificantBits() != 0 || apiKeyId.getLeastSignificantBits() != 0) {
            require(auth, EMAIL_SEND);
        }
        UUID userId = userId(auth);

        var resp = emailSendService.sendBatch(userId, apiKeyId, req);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(resp);
    }
}
