package com.crescendo.emailservice.email_send;

import com.crescendo.security.ApiKeyAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.crescendo.security.AuthenticatedUser.userId;

import java.util.UUID;

/**
 * Public email sending API — authenticated via API key (Bearer re_...).
 * Mirrors Resend's API design for familiarity:
 *
 *   POST /api/v1/emails   — send an email (returns immediately, delivery is async)
 *
 * This endpoint is accessible to both JWT-authenticated dashboard users
 * and API-key-authenticated external services.
 */
@RestController
@RequestMapping("/api/v1/emails")
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
        UUID userId = userId(auth);

        // Resolve API key ID from the request attribute set by ApiKeyAuthenticationFilter.
        // Falls back to nil UUID for JWT-authenticated requests.
        UUID apiKeyId = resolveApiKeyId(request);

        var resp = emailSendService.sendEmail(userId, apiKeyId, req);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(resp);
    }

    private UUID resolveApiKeyId(HttpServletRequest request) {
        Object attr = request.getAttribute(ApiKeyAuthenticationFilter.API_KEY_ID_ATTRIBUTE);
        if (attr instanceof UUID uuid) return uuid;
        return new UUID(0, 0);
    }
}
