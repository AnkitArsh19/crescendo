package com.crescendo.emailservice.email_log;

import com.crescendo.security.ApiKeyAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static com.crescendo.security.AuthenticatedUser.userId;

import java.util.List;
import java.util.UUID;

/**
 * Email tracking / delivery status endpoints.
 * Accessible via both JWT (dashboard) and API key (programmatic).
 *
 *   GET /api/v1/emails           — list recent emails for the authenticated user
 *   GET /api/v1/emails/{emailId} — get a single email's status and details
 */
@RestController
@RequestMapping("/api/v1/emails")
public class EmailLogController {

    private final EmailLogRepository emailLogRepo;

    public EmailLogController(EmailLogRepository emailLogRepo) {
        this.emailLogRepo = emailLogRepo;
    }

    @GetMapping
    public ResponseEntity<List<EmailLogDto.EmailLogResponse>> listEmails(
            Authentication auth,
            HttpServletRequest request) {
        UUID userId = userId(auth);

        // If called with an API key, scope to that key's emails
        UUID apiKeyId = resolveApiKeyId(request);
        List<EmailLog> logs;
        if (apiKeyId != null) {
            logs = emailLogRepo.findByAppKeyIdOrderByCreatedAtDesc(apiKeyId);
        } else {
            logs = emailLogRepo.findByUserIdOrderByCreatedAtDesc(userId);
        }

        return ResponseEntity.ok(logs.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{emailId}")
    public ResponseEntity<EmailLogDto.EmailLogResponse> getEmail(
            @PathVariable UUID emailId,
            Authentication auth) {
        UUID userId = userId(auth);
        EmailLog log = emailLogRepo.findByIdAndUserId(emailId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email not found"));
        return ResponseEntity.ok(toResponse(log));
    }

    private EmailLogDto.EmailLogResponse toResponse(EmailLog log) {
        return new EmailLogDto.EmailLogResponse(
                log.getId(),
                log.getToAddress(),
                log.getFromAddress(),
                log.getSubject(),
                log.getStatus().name(),
                log.getProvider(),
                log.getProviderMessageId(),
                log.getError(),
                log.getTemplateId(),
                log.getCreatedAt(),
                log.getSentAt(),
                log.getOpenedAt(),
                log.getOpenCount(),
                log.getClickCount());
    }

    private UUID resolveApiKeyId(HttpServletRequest request) {
        Object attr = request.getAttribute(ApiKeyAuthenticationFilter.API_KEY_ID_ATTRIBUTE);
        if (attr instanceof UUID uuid) return uuid;
        return null;
    }
}
