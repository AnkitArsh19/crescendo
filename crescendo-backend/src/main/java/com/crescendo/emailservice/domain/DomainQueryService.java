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
    private final com.crescendo.emailservice.email_log.EmailLogRepository emailLogRepo;
    private final com.crescendo.connections.connections_query.Connections_queryRepository connectionsRepo;

    public DomainQueryService(DomainRepository domainRepo,
                              com.crescendo.emailservice.email_log.EmailLogRepository emailLogRepo,
                              com.crescendo.connections.connections_query.Connections_queryRepository connectionsRepo) {
        this.domainRepo = domainRepo;
        this.emailLogRepo = emailLogRepo;
        this.connectionsRepo = connectionsRepo;
    }

    public List<DomainDto.DomainResponse> listDomains(UUID userId) {
        return domainRepo.findByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toRichResponse)
                .toList();
    }

    public DomainDto.DomainResponse getDomain(UUID userId, UUID domainId) {
        Domain domain = domainRepo.findByIdAndUser_Id(domainId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Domain not found"));
        return toRichResponse(domain);
    }

    private DomainDto.DomainResponse toRichResponse(Domain domain) {
        DomainDto.DomainResponse base = DomainCommandService.toResponse(domain);
        
        List<String> warnings = new java.util.ArrayList<>();
        String healthStatus = "GREEN";

        // Check connection validity if personal
        if (domain.getCredentialSource() == com.crescendo.enums.CredentialSource.PERSONAL) {
            if (domain.getEmailProviderConnectionId() == null) {
                warnings.add("Personal provider unavailable, falling back to platform sending");
            } else {
                boolean valid = connectionsRepo.findById(domain.getEmailProviderConnectionId()).isPresent();
                if (!valid) {
                    warnings.add("Personal provider unavailable, falling back to platform sending");
                }
            }
        }

        // Compute health from bounce/complaint over 48 hours
        java.time.Instant fortyEightHoursAgo = java.time.Instant.now().minus(48, java.time.temporal.ChronoUnit.HOURS);
        long totalSends = emailLogRepo.countTotalSendsSince(domain.getUser().getId(), domain.getDomainName(), fortyEightHoursAgo);
        
        if (totalSends > 0) {
            long bounces = emailLogRepo.countStatusSince(domain.getUser().getId(), domain.getDomainName(), com.crescendo.enums.EmailStatus.BOUNCED, fortyEightHoursAgo);
            long complaints = emailLogRepo.countStatusSince(domain.getUser().getId(), domain.getDomainName(), com.crescendo.enums.EmailStatus.COMPLAINED, fortyEightHoursAgo);
            
            double bounceRate = (double) bounces / totalSends;
            double complaintRate = (double) complaints / totalSends;
            
            if (bounceRate > DomainWarmingService.MAX_BOUNCE_RATE || complaintRate > DomainWarmingService.MAX_COMPLAINT_RATE) {
                healthStatus = "RED";
            } else if (bounceRate > (DomainWarmingService.MAX_BOUNCE_RATE / 2.0) || complaintRate > (DomainWarmingService.MAX_COMPLAINT_RATE / 2.0)) {
                healthStatus = "YELLOW";
            }
        }

        return new DomainDto.DomainResponse(
                base.id(), base.domainName(), base.status(), base.requiredDnsRecords(),
                base.createdAt(), base.verifiedAt(), base.spfVerified(), base.dkimVerified(),
                base.dmarcVerified(), base.dailySendCap(), base.warmingStatus(), base.sendReadiness(),
                base.allowedEmailType(), base.credentialSource(), base.emailProviderConnectionId(),
                healthStatus, warnings
        );
    }
}
