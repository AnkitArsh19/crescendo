package com.crescendo.emailservice.domain;

import com.crescendo.emailservice.domain_event.DomainAddedEvent;
import com.crescendo.emailservice.domain_event.DomainVerifiedEvent;
import com.crescendo.enums.DomainStatus;
import com.crescendo.shared.domain.event.DomainEventPublisher;
import com.crescendo.shared.domain.valueobject.DomainName;
import com.crescendo.user.user_command.User_command;
import com.crescendo.user.user_command.User_commandRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Write-side service for domain management.
 *
 * Lifecycle: add domain → user creates DNS TXT record → verify domain.
 */
@Service
@Transactional
public class DomainCommandService {

    private static final int MAX_DOMAINS_PER_USER = 10;
    private static final int TOKEN_BYTES = 16;

    private final DomainRepository domainRepo;
    private final User_commandRepository userRepo;
    private final DnsVerificationService dnsService;
    private final DomainEventPublisher eventPublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    public DomainCommandService(DomainRepository domainRepo,
                                User_commandRepository userRepo,
                                DnsVerificationService dnsService,
                                DomainEventPublisher eventPublisher) {
        this.domainRepo = domainRepo;
        this.userRepo = userRepo;
        this.dnsService = dnsService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Adds a new domain for the user. Generates a verification token and returns
     * the DNS record the user must create before calling verify.
     */
    public DomainDto.DomainResponse addDomain(UUID userId, DomainDto.AddDomainRequest req) {
        User_command user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        DomainName domainName = DomainName.of(req.domainName());

        // Check duplicate
        domainRepo.findByDomainNameAndUserId(domainName.value(), userId).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Domain already registered: " + domainName.value());
        });

        // Enforce limit
        long count = domainRepo.findByUser_IdOrderByCreatedAtDesc(userId).size();
        if (count >= MAX_DOMAINS_PER_USER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Maximum domains reached (" + MAX_DOMAINS_PER_USER + ")");
        }

        String token = generateToken();
        UUID domainId = UUID.randomUUID();

        Domain domain = new Domain(domainId, user, domainName, List.of(token), DomainStatus.PENDING);
        domainRepo.save(domain);

        eventPublisher.publish(new DomainAddedEvent(domainId, userId, domainName.value()));

        return toResponse(domain);
    }

    /**
     * Triggers DNS TXT record verification for the domain.
     * Checks _crescendo-verify.<domain> for the expected token value.
     */
    public DomainDto.DomainResponse verifyDomain(UUID userId, UUID domainId) {
        Domain domain = domainRepo.findByIdAndUser_Id(domainId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Domain not found"));

        if (domain.getStatus() == DomainStatus.VERIFIED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Domain already verified");
        }

        boolean verified = dnsService.verifyDomainTxtRecord(
                domain.getDomainName(), domain.getVerificationTokens());

        if (verified) {
            domain.setStatus(DomainStatus.VERIFIED);
            domain.setVerifiedAt(Instant.now());
            eventPublisher.publish(new DomainVerifiedEvent(domainId, domain.getDomainName()));
        } else {
            domain.setStatus(DomainStatus.FAILED);
        }

        return toResponse(domain);
    }

    /**
     * Deletes a domain registration.
     */
    public void deleteDomain(UUID userId, UUID domainId) {
        Domain domain = domainRepo.findByIdAndUser_Id(domainId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Domain not found"));
        domainRepo.delete(domain);
    }

    // INTERNAL

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static DomainDto.DomainResponse toResponse(Domain domain) {
        List<DomainDto.DnsRecord> dnsRecords = domain.getVerificationTokens().stream()
                .map(token -> new DomainDto.DnsRecord(
                        "TXT",
                        DnsVerificationService.TXT_RECORD_PREFIX + domain.getDomainName(),
                        DnsVerificationService.TOKEN_VALUE_PREFIX + token
                ))
                .toList();

        return new DomainDto.DomainResponse(
                domain.getId(),
                domain.getDomainName(),
                domain.getStatus().name(),
                dnsRecords,
                domain.getCreatedAt(),
                domain.getVerifiedAt()
        );
    }
}
