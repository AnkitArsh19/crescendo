package com.crescendo.publicapi.email;

import com.crescendo.emailservice.domain.DomainCommandService;
import com.crescendo.emailservice.domain.DomainDto;
import com.crescendo.emailservice.domain.DomainQueryService;
import com.crescendo.publicapi.PublicApiScopes;
import com.crescendo.publicapi.common.CursorUtils;
import com.crescendo.publicapi.common.PublicPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.crescendo.security.AuthenticatedUser.userId;

@RestController
@RequestMapping("/api/v1/domains")
@Tag(name = "Domains", description = "Public API for managing email domains")
public class PublicDomainController {

    private final DomainCommandService commandService;
    private final DomainQueryService queryService;

    public PublicDomainController(DomainCommandService commandService, DomainQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @PostMapping
    @Operation(summary = "Add a domain", description = "Registers a new domain. Requires domain:write scope.")
    public ResponseEntity<DomainDto.DomainResponse> addDomain(
            @Valid @RequestBody DomainDto.AddDomainRequest req,
            Authentication auth) {
        PublicApiScopes.require(auth, PublicApiScopes.DOMAIN_WRITE);
        var resp = commandService.addDomain(userId(auth), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping
    @Operation(summary = "List domains", description = "Lists all registered domains. Requires domain:read scope.")
    public ResponseEntity<PublicPage<DomainDto.DomainResponse>> listDomains(
            @RequestParam(required = false, defaultValue = "100") int limit,
            @RequestParam(required = false) String after,
            Authentication auth) {
        PublicApiScopes.require(auth, PublicApiScopes.DOMAIN_READ);
        
        List<DomainDto.DomainResponse> all = queryService.listDomains(userId(auth));
        
        int offset = CursorUtils.decodeOffset(after);
        int toIndex = Math.min(offset + limit, all.size());
        List<DomainDto.DomainResponse> pageData = offset >= all.size() ? List.of() : all.subList(offset, toIndex);
        
        boolean hasMore = toIndex < all.size();
        String nextCursor = hasMore ? CursorUtils.encodeOffset(toIndex) : null;
        
        return ResponseEntity.ok(new PublicPage<>(pageData, hasMore, nextCursor));
    }

    @GetMapping("/{domainId}")
    @Operation(summary = "Get domain details", description = "Gets details and DNS records for a domain. Requires domain:read scope.")
    public ResponseEntity<DomainDto.DomainResponse> getDomain(
            @PathVariable UUID domainId,
            Authentication auth) {
        PublicApiScopes.require(auth, PublicApiScopes.DOMAIN_READ);
        return ResponseEntity.ok(queryService.getDomain(userId(auth), domainId));
    }

    @PostMapping("/{domainId}/verify")
    @Operation(summary = "Verify domain DNS", description = "Triggers a DNS verification check. Requires domain:write scope.")
    public ResponseEntity<DomainDto.DomainResponse> verifyDomain(
            @PathVariable UUID domainId,
            Authentication auth) {
        PublicApiScopes.require(auth, PublicApiScopes.DOMAIN_WRITE);
        var resp = commandService.verifyDomain(userId(auth), domainId);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/{domainId}")
    @Operation(summary = "Delete domain", description = "Deletes a domain. Requires domain:write scope.")
    public ResponseEntity<Void> deleteDomain(
            @PathVariable UUID domainId,
            Authentication auth) {
        PublicApiScopes.require(auth, PublicApiScopes.DOMAIN_WRITE);
        commandService.deleteDomain(userId(auth), domainId);
        return ResponseEntity.noContent().build();
    }
}
