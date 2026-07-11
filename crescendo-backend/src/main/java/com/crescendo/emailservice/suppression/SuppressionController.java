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

    @PostMapping(value = "/settings/suppressions/import", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> importJson(@RequestBody SuppressionDto.ImportSuppressionsRequest req, Authentication auth) {
        if (req.emails() != null) {
            suppressionService.importSuppressions(req.emails(), "IMPORTED", userId(auth));
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/settings/suppressions/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> importCsv(@RequestParam("file") org.springframework.web.multipart.MultipartFile file, Authentication auth) {
        try {
            String content = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
            String[] lines = content.split("\\r?\\n");
            if (lines.length == 0) return ResponseEntity.badRequest().build();
            
            // Find email column index
            int emailColIdx = -1;
            String[] headers = lines[0].split(",");
            for (int i = 0; i < headers.length; i++) {
                String header = headers[i].trim().toLowerCase();
                if (header.contains("email") || header.contains("e-mail")) {
                    emailColIdx = i;
                    break;
                }
            }

            java.util.List<String> emails = new java.util.ArrayList<>();
            int startIndex = emailColIdx != -1 ? 1 : 0; // Skip header if found
            for (int i = startIndex; i < lines.length; i++) {
                String[] cols = lines[i].split(",");
                if (emailColIdx != -1 && emailColIdx < cols.length) {
                    emails.add(cols[emailColIdx].trim());
                } else {
                    // Fallback: look for an email in any column
                    for (String col : cols) {
                        col = col.trim();
                        if (col.contains("@") && col.contains(".")) {
                            emails.add(col);
                            break;
                        }
                    }
                }
            }
            
            suppressionService.importSuppressions(emails, "IMPORTED", userId(auth));
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
