package com.crescendo.emailservice.domain;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class DomainDto {

    private DomainDto() {}

    public record AddDomainRequest(
            @NotBlank String domainName,
            com.crescendo.enums.AllowedEmailType allowedEmailType,
            com.crescendo.enums.CredentialSource credentialSource,
            UUID emailProviderConnectionId
    ) {}

    public record DomainResponse(
            UUID id,
            String domainName,
            String status,
            List<DnsRecord> requiredDnsRecords,
            Instant createdAt,
            Instant verifiedAt,
            boolean spfVerified,
            boolean dkimVerified,
            boolean dmarcVerified,
            int dailySendCap,
            String warmingStatus,
            String sendReadiness,
            String allowedEmailType,
            String credentialSource,
            UUID emailProviderConnectionId,
            String healthStatus,
            List<String> warnings
    ) {}

    /// DNS record the user must add to verify ownership. Presented in the response
    /// so the user can copy-paste it into their DNS provider.
    public record DnsRecord(
            String type,
            String name,
            String value
    ) {}
}
