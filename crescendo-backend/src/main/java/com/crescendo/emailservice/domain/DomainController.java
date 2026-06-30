package com.crescendo.emailservice.domain;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.crescendo.security.AuthenticatedUser.userId;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Domain management endpoints under /settings/domains.
 *
 *   POST   /settings/domains              — add a new custom domain
 *   GET    /settings/domains              — list all domains for user
 *   GET    /settings/domains/{id}         — get domain detail + DNS records
 *   POST   /settings/domains/{id}/verify  — trigger DNS verification
 *   DELETE /settings/domains/{id}         — remove domain
 */
@RestController
@RequestMapping("/settings/domains")
public class DomainController {

    private final DomainCommandService commandService;
    private final DomainQueryService queryService;
    private final DomainConnectService domainConnectService;

    public DomainController(DomainCommandService commandService,
                            DomainQueryService queryService,
                            DomainConnectService domainConnectService) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.domainConnectService = domainConnectService;
    }

    @PostMapping
    public ResponseEntity<DomainDto.DomainResponse> addDomain(
            @Valid @RequestBody DomainDto.AddDomainRequest req,
            Authentication auth) {
        var resp = commandService.addDomain(userId(auth), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping
    public ResponseEntity<List<DomainDto.DomainResponse>> listDomains(Authentication auth) {
        return ResponseEntity.ok(queryService.listDomains(userId(auth)));
    }

    @GetMapping("/{domainId}")
    public ResponseEntity<DomainDto.DomainResponse> getDomain(
            @PathVariable UUID domainId,
            Authentication auth) {
        return ResponseEntity.ok(queryService.getDomain(userId(auth), domainId));
    }

    @PostMapping("/{domainId}/verify")
    public ResponseEntity<DomainDto.DomainResponse> verifyDomain(
            @PathVariable UUID domainId,
            Authentication auth) {
        var resp = commandService.verifyDomain(userId(auth), domainId);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/{domainId}")
    public ResponseEntity<Void> deleteDomain(
            @PathVariable UUID domainId,
            Authentication auth) {
        commandService.deleteDomain(userId(auth), domainId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Gets the Domain Connect synchronous redirect URL for a specific domain.
     * Returns 404 if the domain is not found, or a specific message if Domain Connect is unsupported.
     */
    @GetMapping("/{id}/domain-connect")
    public ResponseEntity<?> getDomainConnectUrl(@PathVariable UUID id, Authentication auth) {
        var domainDto = queryService.getDomain(userId(auth), id);
        return domainConnectService.buildSyncUrl(domainDto.domainName())
                .map(url -> ResponseEntity.ok().body(Map.of("url", url)))
                .orElseGet(() -> ResponseEntity.badRequest()
                        .body(Map.of("error", "Domain Connect is not supported by the DNS provider for this domain.")));
    }

}
