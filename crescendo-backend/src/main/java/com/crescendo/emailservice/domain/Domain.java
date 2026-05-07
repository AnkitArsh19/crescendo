package com.crescendo.emailservice.domain;

import com.crescendo.enums.DomainStatus;
import com.crescendo.shared.domain.valueobject.DomainName;
import com.crescendo.user.user_command.User_command;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Entity to store the domain for the email service.
 * The user provides the custom domain. Uses DomainName value object for validation.
 */
@Entity
@Table(name = "domain",
    indexes = {
        @Index(name = "idx_domain_user", columnList = "user_id"),
        @Index(name = "idx_domain_status", columnList = "status")
    })
public class Domain {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, foreignKey = @ForeignKey(name = "fk_domain_user"))
    private User_command user;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "domain_name", nullable = false, length = 255))
    private DomainName domainName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "verification_tokens", columnDefinition = "jsonb", nullable = false)
    private List<String> verificationTokens;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DomainStatus status;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    @Column(name = "verifiedAt")
    private Instant verifiedAt;

    public Domain() {
    }

    public Domain(UUID id, User_command user, DomainName domainName, List<String> verificationTokens, DomainStatus status) {
        this.id = id;
        this.user = user;
        this.domainName = domainName;
        this.verificationTokens = verificationTokens;
        this.status = status;
    }

    /**
     * Convenience constructor accepting raw string for domainName.
     */
    public Domain(UUID id, User_command user, String domainName, List<String> verificationTokens, DomainStatus status) {
        this(id, user, DomainName.of(domainName), verificationTokens, status);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User_command getUser() {
        return user;
    }

    public void setUser(User_command user) {
        this.user = user;
    }

    public DomainName getDomainNameVO() {
        return domainName;
    }

    /**
     * Returns raw domain name string for compatibility.
     */
    public String getDomainName() {
        return domainName != null ? domainName.value() : null;
    }

    public void setDomainName(DomainName domainName) {
        this.domainName = domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = DomainName.of(domainName);
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
