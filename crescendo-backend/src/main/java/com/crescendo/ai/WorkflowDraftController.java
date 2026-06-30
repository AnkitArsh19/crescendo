package com.crescendo.ai;

import com.crescendo.security.AppUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.crescendo.publicapi.PublicApiScopes.AI_BUILD;
import static com.crescendo.security.AuthenticatedUser.userId;

/**
 * Internal AI endpoints — proxies to the configured Python AI microservice.
 *
 * Route: /ai/** (internal feature route, NOT part of the versioned public API)
 *
 * Auth: accepts EITHER a regular user JWT session OR a public API key with the
 * {@code ai:build} scope. Any logged-in user can use the NL workflow builder
 * from the dashboard UI without needing a developer API key.
 *
 * The Python service URL and shared service token are configured via:
 *   crescendo.python-ai.base-url    (blank = feature disabled, returns 503)
 *   crescendo.python-ai.service-token
 */
@RestController
@RequestMapping("/ai")
public class WorkflowDraftController {


    private final String pythonBaseUrl;
    private final String pythonServiceToken;

    public WorkflowDraftController(
            @Value("${crescendo.python-ai.base-url:}") String pythonBaseUrl,
            @Value("${crescendo.python-ai.service-token:}") String pythonServiceToken) {
        this.pythonBaseUrl = pythonBaseUrl;
        this.pythonServiceToken = pythonServiceToken;
    }

    @PostMapping("/workflow-drafts")
    public ResponseEntity<Map<String, Object>> createWorkflowDraft(
            @Valid @RequestBody WorkflowDraftRequest request,
            Authentication auth) {

        // Accept either a regular user JWT session or an API key with ai:build scope.
        UUID resolvedUserId = resolveUserId(auth);

        if (pythonBaseUrl == null || pythonBaseUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI workflow builder is not available yet.");
        }

        Map<String, Object> context = request.context() != null ? new HashMap<>(request.context()) : new HashMap<>();
        
        Map<String, Object> body = Map.of(
                "userId",  resolvedUserId.toString(),
                "prompt",  request.prompt(),
                "context", context
        );

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(trimTrailingSlash(pythonBaseUrl))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT,       MediaType.APPLICATION_JSON_VALUE);

        if (pythonServiceToken != null && !pythonServiceToken.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + pythonServiceToken);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> response = builder.build()
                .post()
                .uri("/v1/workflow-drafts")
                .body(body)
                .retrieve()
                .body(Map.class);

        return ResponseEntity.ok(response != null ? response : Map.of());
    }

    /**
     * Resolves the caller's user ID regardless of whether they authenticated via:
     *  - a regular user JWT session (AppUserDetails principal), or
     *  - a public API key with the ai:build scope (PublicApiPrincipal).
     *
     * For API key callers, additionally verifies they carry the ai:build scope.
     */
    private UUID resolveUserId(Authentication auth) {
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        // Regular logged-in user — no scope check needed
        if (auth.getPrincipal() instanceof AppUserDetails) {
            return userId(auth);
        }
        // API key caller — must carry ai:build scope
        boolean hasScope = auth.getAuthorities().stream()
                .anyMatch(a -> ("SCOPE_" + AI_BUILD).equals(a.getAuthority()));
        if (!hasScope) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "API key is missing required scope: " + AI_BUILD);
        }
        return userId(auth);
    }

    private String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public record WorkflowDraftRequest(
            @NotBlank @Size(max = 8000) String prompt,
            Map<String, Object> context
    ) {}
}
