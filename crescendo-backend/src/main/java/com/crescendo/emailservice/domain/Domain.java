package com.crescendo.emailservice.domain;

import com.crescendo.enums.DomainStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "domain")
public class Domain {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "domain_name", nullable = false)
    private String domainName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "verification_tokens", columnDefinition = "jsonb", nullable = false)
    private List<String> verificationTokens;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DomainStatus status;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    @Column(name = "verifiedAt")
    private Instant verifiedAt;

    public Domain() {
    }

    public Domain(UUID id, UUID userId, String domainName, List<String> verificationTokens, DomainStatus status, Instant createdAt, Instant verifiedAt) {
        this.id = id;
        this.userId = userId;
        this.domainName = domainName;
        this.verificationTokens = verificationTokens;
        this.status = status;
        this.createdAt = createdAt;
        this.verifiedAt = verifiedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public List<String> getVerificationTokens() {
        return verificationTokens;
    }

    public void setVerificationTokens(List<String> verificationTokens) {
        this.verificationTokens = verificationTokens;
    }

    public DomainStatus getStatus() {
        return status;
    }

    public void setStatus(DomainStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
}
