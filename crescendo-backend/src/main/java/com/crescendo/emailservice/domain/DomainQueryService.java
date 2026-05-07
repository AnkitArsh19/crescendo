package com.crescendo.emailservice.domain;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Read-side service for domains.
 * Domain is command-only entity (no query projection) — reads go through the command repo.
 */
@Service
@Transactional(readOnly = true)
public class DomainQueryService {

    private final DomainRepository domainRepo;

    public DomainQueryService(DomainRepository domainRepo) {
        this.domainRepo = domainRepo;
    }

    public List<DomainDto.DomainResponse> listDomains(UUID userId) {
        return domainRepo.findByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(DomainCommandService::toResponse)
                .toList();
    }

    public DomainDto.DomainResponse getDomain(UUID userId, UUID domainId) {
        Domain domain = domainRepo.findByIdAndUser_Id(domainId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Domain not found"));
        return DomainCommandService.toResponse(domain);
    }
}
