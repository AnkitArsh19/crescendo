package com.crescendo.publicapi.email;

import com.crescendo.emailservice.suppression.EmailSuppressionService;
import com.crescendo.emailservice.suppression.SuppressionDto;
import com.crescendo.publicapi.PublicApiScopes;
import com.crescendo.publicapi.common.CursorUtils;
import com.crescendo.publicapi.common.PublicPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.crescendo.security.AuthenticatedUser.userId;

@RestController
@RequestMapping("/api/v1/suppressions")
@Tag(name = "Suppressions", description = "Public API for managing email suppressions")
public class PublicSuppressionController {

    private final EmailSuppressionService suppressionService;

    public PublicSuppressionController(EmailSuppressionService suppressionService) {
        this.suppressionService = suppressionService;
    }

    @GetMapping
    @Operation(summary = "List suppressions", operationId = "listSuppressions", description = "Lists all suppressed email addresses. Requires suppression:read scope.")
    public ResponseEntity<PublicPage<SuppressionDto.SuppressionResponse>> list(
            @RequestParam(required = false, defaultValue = "100") int limit,
            @RequestParam(required = false) String after,
            Authentication auth) {
        PublicApiScopes.require(auth, PublicApiScopes.SUPPRESSION_READ);
        
        List<SuppressionDto.SuppressionResponse> all = suppressionService.list(userId(auth))
                .stream()
                .map(s -> new SuppressionDto.SuppressionResponse(
                        s.getId(), s.getNormalizedEmail(), s.getReason(), s.getCreatedAt()))
                .toList();
        
        int offset = CursorUtils.decodeOffset(after);
        int toIndex = Math.min(offset + limit, all.size());
        List<SuppressionDto.SuppressionResponse> pageData = offset >= all.size() ? List.of() : all.subList(offset, toIndex);
        
        boolean hasMore = toIndex < all.size();
        String nextCursor = hasMore ? CursorUtils.encodeOffset(toIndex) : null;
        
        return ResponseEntity.ok(new PublicPage<>(pageData, hasMore, nextCursor));
    }

    @PostMapping
    @Operation(summary = "Add a suppression", operationId = "addSuppression", description = "Manually suppresses an email address. Requires suppression:write scope.")
    public ResponseEntity<Void> add(
            @Valid @RequestBody SuppressionDto.AddSuppressionRequest req,
            Authentication auth) {
        PublicApiScopes.require(auth, PublicApiScopes.SUPPRESSION_WRITE);
        suppressionService.suppress(userId(auth), req.email(), "MANUAL");
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{suppressionId}")
    @Operation(summary = "Delete a suppression", operationId = "removeSuppression", description = "Removes an email address from the suppression list. Requires suppression:write scope.")
    public ResponseEntity<Void> remove(
            @PathVariable UUID suppressionId,
            Authentication auth) {
        PublicApiScopes.require(auth, PublicApiScopes.SUPPRESSION_WRITE);
        suppressionService.remove(userId(auth), suppressionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/import", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Bulk import suppressions", operationId = "importSuppressions", description = "Imports a bulk list of suppressions. Requires suppression:import scope.")
    public ResponseEntity<Void> importJson(
            @RequestBody SuppressionDto.ImportSuppressionsRequest req,
            Authentication auth) {
        PublicApiScopes.require(auth, PublicApiScopes.SUPPRESSION_IMPORT);
        if (req.emails() != null) {
            suppressionService.importSuppressions(req.emails(), "IMPORTED", userId(auth));
        }
        return ResponseEntity.noContent().build();
    }
}
