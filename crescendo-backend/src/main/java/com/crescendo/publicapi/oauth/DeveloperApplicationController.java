package com.crescendo.publicapi.oauth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import static com.crescendo.security.AuthenticatedUser.userId;

@io.swagger.v3.oas.annotations.Hidden
@RestController
@RequestMapping("/settings/developer-apps")
@PreAuthorize("@accessControl.isFullAccess()")
public class DeveloperApplicationController {
    private final DeveloperApplicationService service;

    public DeveloperApplicationController(DeveloperApplicationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DeveloperApplicationDto.CreatedResponse> create(
            Authentication authentication,
            @Valid @RequestBody DeveloperApplicationDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(userId(authentication), request));
    }

    @GetMapping
    public List<DeveloperApplicationDto.ApplicationResponse> list(Authentication authentication) {
        return service.list(userId(authentication));
    }

    @GetMapping("/{id}")
    public DeveloperApplicationDto.ApplicationResponse get(
            Authentication authentication,
            @PathVariable String id) {
        return service.get(userId(authentication), id);
    }

    @GetMapping("/{id}/usage")
    public Page<DeveloperApplicationDto.UsageResponse> usage(
            Authentication authentication,
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return service.usage(
                userId(authentication),
                id,
                PageRequest.of(Math.max(0, page), Math.max(1, Math.min(100, size)))
        );
    }

    @PatchMapping("/{id}")
    public DeveloperApplicationDto.ApplicationResponse update(
            Authentication authentication,
            @PathVariable String id,
            @Valid @RequestBody DeveloperApplicationDto.UpdateRequest request) {
        return service.update(userId(authentication), id, request);
    }

    @PostMapping("/{id}/rotate-secret")
    public DeveloperApplicationDto.SecretResponse rotateSecret(
            Authentication authentication,
            @PathVariable String id) {
        return service.rotateSecret(userId(authentication), id);
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(
            Authentication authentication,
            @PathVariable String id) {
        service.deactivate(userId(authentication), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable String id) {
        service.delete(userId(authentication), id);
        return ResponseEntity.noContent().build();
    }
}
