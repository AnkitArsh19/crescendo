package com.crescendo.emailservice.domain;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class DomainDto {

    private DomainDto() {}

    public record AddDomainRequest(
            @NotBlank String domainName
    ) {}

    public record DomainResponse(
            UUID id,
            String domainName,
            String status,
            List<DnsRecord> requiredDnsRecords,
            Instant createdAt,
            Instant verifiedAt
    ) {}

    /// DNS record the user must add to verify ownership. Presented in the response
    /// so the user can copy-paste it into their DNS provider.
    public record DnsRecord(
            String type,
            String name,
            String value
    ) {}
}
