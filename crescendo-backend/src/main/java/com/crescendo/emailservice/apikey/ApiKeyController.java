package com.crescendo.emailservice.apikey;

import com.crescendo.emailservice.apikey.key_command.ApiKey_commandService;
import com.crescendo.emailservice.apikey.key_query.ApiKey_queryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.crescendo.security.AuthenticatedUser.userId;

import java.util.List;
import java.util.UUID;

/**
 * Authenticated API key management endpoints under /settings/api-keys.
 *
 *   POST   /settings/api-keys           — create new API key (returns plain key ONCE)
 *   GET    /settings/api-keys           — list active keys (prefix + metadata only)
 *   GET    /settings/api-keys/{id}      — get key detail
 *   DELETE /settings/api-keys/{id}      — revoke key
 */
@RestController
@RequestMapping("/settings/api-keys")
public class ApiKeyController {

    private final ApiKey_commandService commandService;
    private final ApiKey_queryService queryService;

    public ApiKeyController(ApiKey_commandService commandService,
                            ApiKey_queryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<ApiKeyDto.ApiKeyCreatedResponse> createApiKey(
            @Valid @RequestBody ApiKeyDto.CreateApiKeyRequest req,
            Authentication auth) {
        var resp = commandService.createApiKey(userId(auth), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping
    public ResponseEntity<List<ApiKeyDto.ApiKeyResponse>> listApiKeys(Authentication auth) {
        return ResponseEntity.ok(queryService.listApiKeys(userId(auth)));
    }

    @GetMapping("/{apiKeyId}")
    public ResponseEntity<ApiKeyDto.ApiKeyResponse> getApiKey(
            @PathVariable UUID apiKeyId,
            Authentication auth) {
        return ResponseEntity.ok(queryService.getApiKey(userId(auth), apiKeyId));
    }

    @GetMapping("/{apiKeyId}/usage")
    public ResponseEntity<Page<ApiKeyDto.ApiKeyUsageResponse>> listUsage(
            @PathVariable UUID apiKeyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication auth) {
        return ResponseEntity.ok(queryService.listUsage(
                userId(auth),
                apiKeyId,
                PageRequest.of(page, Math.min(size, 100))));
    }

    @DeleteMapping("/{apiKeyId}")
    public ResponseEntity<Void> revokeApiKey(
            @PathVariable UUID apiKeyId,
            Authentication auth) {
        commandService.revokeApiKey(userId(auth), apiKeyId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{apiKeyId}/rotate")
    public ResponseEntity<ApiKeyDto.ApiKeyCreatedResponse> rotateApiKey(
            @PathVariable UUID apiKeyId,
            @Valid @RequestBody(required = false) ApiKeyDto.RotateApiKeyRequest req,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commandService.rotateApiKey(userId(auth), apiKeyId, req));
    }

}
