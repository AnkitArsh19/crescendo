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
    private final com.crescendo.emailservice.provider.EmailProviderResolver emailProviderResolver;
    private final SecureRandom secureRandom = new SecureRandom();

    public DomainCommandService(DomainRepository domainRepo,
                                User_commandRepository userRepo,
                                DnsVerificationService dnsService,
                                DomainEventPublisher eventPublisher,
                                com.crescendo.emailservice.provider.EmailProviderResolver emailProviderResolver) {
        this.domainRepo = domainRepo;
        this.userRepo = userRepo;
        this.dnsService = dnsService;
        this.eventPublisher = eventPublisher;
        this.emailProviderResolver = emailProviderResolver;
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
        
        com.crescendo.enums.AllowedEmailType type = req.allowedEmailType() != null ? req.allowedEmailType() : com.crescendo.enums.AllowedEmailType.TRANSACTIONAL_ONLY;
        com.crescendo.enums.CredentialSource source = req.credentialSource() != null ? req.credentialSource() : com.crescendo.enums.CredentialSource.PLATFORM;

        Domain domain = new Domain(domainId, user, domainName, List.of(token), DomainStatus.PENDING, type, source, req.emailProviderConnectionId());
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

        boolean ownershipVerified = domain.getStatus() == DomainStatus.VERIFIED || dnsService.verifyDomainTxtRecord(
                domain.getDomainName(), domain.getVerificationTokens());

        if (ownershipVerified) {
            String domainToCheck = domain.getDomainName();
            domain.setSpfVerified(dnsService.verifySpf(domainToCheck));
            domain.setDkimVerified(dnsService.verifyDkim("crescendo", domainToCheck));
            domain.setDmarcVerified(dnsService.verifyDmarc(domainToCheck));

            if (domain.getStatus() != DomainStatus.VERIFIED) {
                domain.setStatus(DomainStatus.VERIFIED);
                domain.setVerifiedAt(Instant.now());
                eventPublisher.publish(new DomainVerifiedEvent(domainId, domain.getDomainName()));

                // Claim domain: if any other user had this domain registered or verified, mark theirs as failed.
                List<Domain> otherDomains = domainRepo.findByDomainName(domainToCheck);
                for (Domain other : otherDomains) {
                    if (!other.getId().equals(domain.getId())) {
                        other.setStatus(DomainStatus.FAILED);
                        domainRepo.save(other);
                    }
                }
            } else {
                domain.updateReadiness();
            }
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
        
        if (domain.getEmailProviderConnectionId() != null) {
            throw new IllegalStateException("Cannot delete a domain that is currently bound to an email provider connection.");
        }
        
        domainRepo.delete(domain);
    }

    public DomainDto.DomainResponse patchDomain(UUID userId, UUID domainId, DomainDto.PatchDomainRequest req) {
        Domain domain = domainRepo.findByIdAndUser_Id(domainId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Domain not found"));

        if (req.trackingEnabled() != null) domain.setTrackingEnabled(req.trackingEnabled());
        if (req.unsubscribeLogoUrl() != null) domain.setUnsubscribeLogoUrl(req.unsubscribeLogoUrl());
        if (req.unsubscribePrimaryColor() != null) domain.setUnsubscribePrimaryColor(req.unsubscribePrimaryColor());
        if (req.unsubscribeCopy() != null) domain.setUnsubscribeCopy(req.unsubscribeCopy());
        if (req.bimiLogoUrl() != null) domain.setBimiLogoUrl(req.bimiLogoUrl());
        if (req.bimiVmcUrl() != null) domain.setBimiVmcUrl(req.bimiVmcUrl());

        return toResponse(domain);
    }

    /**
     * Initiates a domain ownership claim.
     */
    public void initiateClaim(UUID userId, UUID domainId, DomainDto.ClaimDomainRequest req) {
        Domain domain = domainRepo.findByIdAndUser_Id(domainId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Domain not found"));

        if (domain.getStatus() != DomainStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Domain must be in PENDING state to initiate claim");
        }

        if ("DNS".equalsIgnoreCase(req.method())) {
            // DNS method doesn't need to do anything as the token is already in verificationTokens
            return;
        } else if ("EMAIL".equalsIgnoreCase(req.method())) {
            String email = req.emailAddress();
            if (email == null || email.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "emailAddress is required for EMAIL method");
            }
            
            String domainNameStr = domain.getDomainName().toLowerCase();
            String emailLower = email.toLowerCase();
            
            if (!emailLower.endsWith("@" + domainNameStr)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email must belong to the domain");
            }
            
            String prefix = emailLower.substring(0, emailLower.indexOf("@"));
            if (!List.of("admin", "postmaster", "hostmaster", "webmaster").contains(prefix)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email must be a standard administrative address (admin, postmaster, etc.)");
            }
            
            String token = generateToken();
            List<String> tokens = new java.util.ArrayList<>(domain.getVerificationTokens());
            tokens.add(token);
            domain.setVerificationTokens(tokens);
            domainRepo.save(domain);
            
            com.crescendo.emailservice.provider.EmailProvider provider = emailProviderResolver.resolve(null);
            String html = "<p>You have requested to claim the domain <strong>" + domainNameStr + "</strong> on Crescendo.</p>" +
                          "<p>Your claim token is: <strong>" + token + "</strong></p>" +
                          "<p>Enter this token in your Crescendo dashboard to complete the domain transfer.</p>";
                          
            com.crescendo.emailservice.provider.EmailMessage msg = new com.crescendo.emailservice.provider.EmailMessage(
                    email,
                    "noreply@crescendo.run",
                    "Claim Domain Ownership - Crescendo",
                    html,
                    null
            );
            
            provider.send(msg);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid method. Must be DNS or EMAIL");
        }
    }

    /**
     * Completes a domain ownership claim.
     */
    public DomainDto.DomainResponse completeClaim(UUID userId, UUID domainId, DomainDto.CompleteClaimRequest req) {
        Domain domain = domainRepo.findByIdAndUser_Id(domainId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Domain not found"));

        if (domain.getStatus() != DomainStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Domain must be in PENDING state to complete claim");
        }

        // If a token is provided, it's the EMAIL method
        if (req != null && req.token() != null && !req.token().isBlank()) {
            if (!domain.getVerificationTokens().contains(req.token())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid claim token");
            }
            
            // Bypass DNS verification since they proved ownership via admin email
            domain.setSpfVerified(dnsService.verifySpf(domain.getDomainName()));
            domain.setDkimVerified(dnsService.verifyDkim("crescendo", domain.getDomainName()));
            domain.setDmarcVerified(dnsService.verifyDmarc(domain.getDomainName()));
            
            domain.setStatus(DomainStatus.VERIFIED);
            domain.setVerifiedAt(Instant.now());
            eventPublisher.publish(new DomainVerifiedEvent(domainId, domain.getDomainName()));

            // Claim domain: if any other user had this domain registered or verified, mark theirs as failed.
            List<Domain> otherDomains = domainRepo.findByDomainName(domain.getDomainName());
            for (Domain other : otherDomains) {
                if (!other.getId().equals(domain.getId())) {
                    other.setStatus(DomainStatus.FAILED);
                    domainRepo.save(other);
                }
            }
            
            domain.updateReadiness();
            return toResponse(domain);
        } else {
            // DNS method delegates to the existing verifyDomain logic
            return verifyDomain(userId, domainId);
        }
    }

    // INTERNAL

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static DomainDto.DomainResponse toResponse(Domain domain) {
        String exactDomain = domain.getDomainName();
        List<DomainDto.DnsRecord> dnsRecords = new java.util.ArrayList<>();
        
        // Root domain ownership verification
        domain.getVerificationTokens().forEach(token -> {
            dnsRecords.add(new DomainDto.DnsRecord(
                    "TXT",
                    DnsVerificationService.TXT_RECORD_PREFIX + exactDomain,
                    DnsVerificationService.TOKEN_VALUE_PREFIX + token
            ));
        });

        // Exact Domain SPF
        dnsRecords.add(new DomainDto.DnsRecord(
                "TXT",
                exactDomain,
                "v=spf1 include:spf.crescendo.run ~all"
        ));

        // Exact Domain DKIM
        dnsRecords.add(new DomainDto.DnsRecord(
                "TXT",
                "crescendo._domainkey." + exactDomain,
                "v=DKIM1; k=rsa; p=YOUR_PUBLIC_KEY" // Placeholder
        ));

        // Exact Domain DMARC
        dnsRecords.add(new DomainDto.DnsRecord(
                "TXT",
                "_dmarc." + exactDomain,
                "v=DMARC1; p=none;"
        ));

        // BIMI
        if (domain.getBimiLogoUrl() != null && !domain.getBimiLogoUrl().isBlank() && 
            domain.getBimiVmcUrl() != null && !domain.getBimiVmcUrl().isBlank()) {
            dnsRecords.add(new DomainDto.DnsRecord(
                    "TXT",
                    "default._bimi." + exactDomain,
                    "v=BIMI1; l=" + domain.getBimiLogoUrl() + "; a=" + domain.getBimiVmcUrl() + ";"
            ));
        }

        return new DomainDto.DomainResponse(
                domain.getId(),
                domain.getDomainName(),
                domain.getStatus().name(),
                dnsRecords,
                domain.getCreatedAt(),
                domain.getVerifiedAt(),
                domain.isSpfVerified(),
                domain.isDkimVerified(),
                domain.isDmarcVerified(),
                domain.getDailySendCap(),
                domain.getWarmingStatus().name(),
                domain.getSendReadiness().name(),
                domain.getAllowedEmailType().name(),
                domain.getCredentialSource().name(),
                domain.getEmailProviderConnectionId(),
                "GREEN",
                java.util.Collections.emptyList(),
                domain.isTrackingEnabled(),
                domain.getUnsubscribeLogoUrl(),
                domain.getUnsubscribePrimaryColor(),
                domain.getUnsubscribeCopy(),
                domain.getBimiLogoUrl(),
                domain.getBimiVmcUrl()
        );
    }
}
