package com.crescendo.emailservice.suppression;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.crescendo.security.RateLimitingService;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

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
    private final RateLimitingService rateLimitingService;

    public SuppressionController(EmailSuppressionService suppressionService, RateLimitingService rateLimitingService) {
        this.suppressionService = suppressionService;
        this.rateLimitingService = rateLimitingService;
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

    @PostMapping(value = "/settings/suppressions/import", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> importJson(@RequestBody SuppressionDto.ImportSuppressionsRequest req, Authentication auth) {
        if (req.emails() != null) {
            suppressionService.importSuppressions(req.emails(), "IMPORTED", userId(auth));
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/settings/suppressions/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> importCsv(@RequestParam("file") org.springframework.web.multipart.MultipartFile file, Authentication auth) {
        UUID uId = userId(auth);

        // 1. Rate Limit (2 requests per minute)
        if (rateLimitingService.isRateLimited("suppressions:import", uId.toString(), 2, Duration.ofMinutes(1))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded for CSV import");
        }

        // 2. Strict Size Limit (10MB)
        if (file.getSize() > 10L * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "CSV file exceeds 10MB limit");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                return ResponseEntity.badRequest().build();
            }

            // Find email column index
            int emailColIdx = -1;
            String[] headers = headerLine.split(",");
            for (int i = 0; i < headers.length; i++) {
                String header = headers[i].trim().toLowerCase();
                if (header.contains("email") || header.contains("e-mail")) {
                    emailColIdx = i;
                    break;
                }
            }

            java.util.List<String> emails = new java.util.ArrayList<>();
            if (emailColIdx != -1) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String[] cols = line.split(",");
                    if (cols.length > emailColIdx) {
                        String e = cols[emailColIdx].trim();
                        if (!e.isBlank()) emails.add(e);
                    }
                }
            } else {
                // If no header, just assume single column or look for an email pattern
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String[] cols = line.split(",");
                    for (String col : cols) {
                        String e = col.trim();
                        if (e.contains("@")) {
                            emails.add(e);
                            break;
                        }
                    }
                }
            }

            if (!emails.isEmpty()) {
                suppressionService.importSuppressions(emails, "IMPORTED", uId);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
