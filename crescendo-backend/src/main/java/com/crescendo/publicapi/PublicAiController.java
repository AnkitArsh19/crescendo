package com.crescendo.publicapi;

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

import java.util.Map;

import static com.crescendo.publicapi.PublicApiScopes.AI_BUILD;
import static com.crescendo.publicapi.PublicApiScopes.require;
import static com.crescendo.security.AuthenticatedUser.userId;

@RestController
@RequestMapping("/api/v1/ai")
public class PublicAiController {
    private final String pythonBaseUrl;
    private final String pythonServiceToken;

    public PublicAiController(@Value("${crescendo.python-ai.base-url:}") String pythonBaseUrl,
                              @Value("${crescendo.python-ai.service-token:}") String pythonServiceToken) {
        this.pythonBaseUrl = pythonBaseUrl;
        this.pythonServiceToken = pythonServiceToken;
    }

    @PostMapping("/workflow-drafts")
    public ResponseEntity<Map<String, Object>> createWorkflowDraft(
            @Valid @RequestBody WorkflowDraftRequest request,
            Authentication auth) {
        require(auth, AI_BUILD);
        if (pythonBaseUrl == null || pythonBaseUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Python AI service is not configured");
        }

        Map<String, Object> body = Map.of(
                "userId", userId(auth).toString(),
                "prompt", request.prompt(),
                "context", request.context() != null ? request.context() : Map.of()
        );

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(trim(pythonBaseUrl))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
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

    private String trim(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public record WorkflowDraftRequest(
            @NotBlank @Size(max = 8000) String prompt,
            Map<String, Object> context
    ) {}
}
