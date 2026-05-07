package com.crescendo.emailservice.suppression;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.crescendo.security.AuthenticatedUser.userId;

import java.util.List;
import java.util.UUID;

/**
 * Suppression list management (authenticated) and one-click unsubscribe
 * (public).
 *
 * Authenticated — /settings/suppressions:
 * GET /settings/suppressions — list all suppressed addresses
 * POST /settings/suppressions — manually suppress an address
 * DELETE /settings/suppressions/{id} — remove from suppression list
 *
 * Public — no auth required:
 * GET /unsubscribe?token={emailId} — one-click unsubscribe from
 * List-Unsubscribe link
 */
@RestController
public class SuppressionController {

    private final EmailSuppressionService suppressionService;

    public SuppressionController(EmailSuppressionService suppressionService) {
        this.suppressionService = suppressionService;
    }

    @GetMapping("/settings/suppressions")
    public ResponseEntity<List<SuppressionDto.SuppressionResponse>> list(Authentication auth) {
        List<SuppressionDto.SuppressionResponse> result = suppressionService.list(userId(auth))
                .stream()
                .map(s -> new SuppressionDto.SuppressionResponse(
                        s.getId(), s.getNormalizedEmail(), s.getReason(), s.getCreatedAt()))
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/settings/suppressions")
    public ResponseEntity<Void> add(@Valid @RequestBody SuppressionDto.AddSuppressionRequest req,
            Authentication auth) {
        suppressionService.suppress(userId(auth), req.email(), "MANUAL");
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/settings/suppressions/{suppressionId}")
    public ResponseEntity<Void> remove(@PathVariable UUID suppressionId, Authentication auth) {
        suppressionService.remove(userId(auth), suppressionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Public one-click unsubscribe page. The token is the emailId (UUID) we sent
     * in the List-Unsubscribe header, so no auth mechanism or guessable address is
     * exposed in the URL.
     */
    @GetMapping(value = "/unsubscribe", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> unsubscribe(@RequestParam String token) {
        try {
            UUID emailId = UUID.fromString(token);
            boolean found = suppressionService.unsubscribeByEmailLogId(emailId);
            return ResponseEntity.ok(found ? CONFIRMED_HTML : NOT_FOUND_HTML);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("<p>Invalid unsubscribe link.</p>");
        }
    }

    private static final String CONFIRMED_HTML = """
            <!DOCTYPE html>
            <html lang="en">
            <head><meta charset="UTF-8"><title>Unsubscribed</title>
            <style>body{font-family:sans-serif;text-align:center;padding:80px;color:#222}
            h2{color:#16a34a}</style></head>
            <body>
              <h2>You have been unsubscribed</h2>
              <p>You will no longer receive emails from this sender.</p>
            </body></html>
            """;

    private static final String NOT_FOUND_HTML = """
            <!DOCTYPE html>
            <html lang="en">
            <head><meta charset="UTF-8"><title>Link Not Found</title>
            <style>body{font-family:sans-serif;text-align:center;padding:80px;color:#222}</style></head>
            <body>
              <h2>Link not found</h2>
              <p>This unsubscribe link may have already been used or is invalid.</p>
            </body></html>
            """;
}
