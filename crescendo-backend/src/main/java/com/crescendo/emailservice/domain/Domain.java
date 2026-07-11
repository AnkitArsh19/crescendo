package com.crescendo.emailservice.domain;

import com.crescendo.enums.DomainStatus;
import com.crescendo.enums.AllowedEmailType;
import com.crescendo.enums.CredentialSource;
import com.crescendo.enums.DomainSendReadiness;
import com.crescendo.enums.WarmingStatus;
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

    @Column(name = "spf_verified", nullable = false)
    private boolean spfVerified = false;

    @Column(name = "dkim_verified", nullable = false)
    private boolean dkimVerified = false;

    @Column(name = "dmarc_verified", nullable = false)
    private boolean dmarcVerified = false;

    @Column(name = "daily_send_cap", nullable = false)
    private int dailySendCap = 50;

    @Enumerated(EnumType.STRING)
    @Column(name = "warming_status", nullable = false, length = 20)
    private WarmingStatus warmingStatus = WarmingStatus.WARMING_UP;

    @Enumerated(EnumType.STRING)
    @Column(name = "send_readiness", nullable = false, length = 30)
    private DomainSendReadiness sendReadiness = DomainSendReadiness.UNVERIFIED;

    @Enumerated(EnumType.STRING)
    @Column(name = "allowed_email_type", nullable = false, length = 30)
    private AllowedEmailType allowedEmailType = AllowedEmailType.TRANSACTIONAL_ONLY;

    @Enumerated(EnumType.STRING)
    @Column(name = "credential_source", nullable = false, length = 20)
    private CredentialSource credentialSource = CredentialSource.PLATFORM;

    @Column(name = "email_provider_connection_id")
    private UUID emailProviderConnectionId;

    @Column(name = "tracking_enabled", nullable = false)
    private boolean trackingEnabled = true;

    @Column(name = "unsubscribe_logo_url", length = 1000)
    private String unsubscribeLogoUrl;

    @Column(name = "unsubscribe_primary_color", length = 20)
    private String unsubscribePrimaryColor;

    @Column(name = "unsubscribe_copy", length = 500)
    private String unsubscribeCopy;

    @Column(name = "bimi_logo_url", length = 1000)
    private String bimiLogoUrl;

    @Column(name = "bimi_vmc_url", length = 1000)
    private String bimiVmcUrl;

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
        this.spfVerified = false;
        this.dkimVerified = false;
        this.dmarcVerified = false;
        this.dailySendCap = 50;
        this.warmingStatus = WarmingStatus.WARMING_UP;
        this.sendReadiness = DomainSendReadiness.UNVERIFIED;
    }

    public Domain(UUID id, User_command user, DomainName domainName, List<String> verificationTokens, DomainStatus status,
                  AllowedEmailType allowedEmailType, CredentialSource credentialSource, UUID emailProviderConnectionId) {
        this(id, user, domainName, verificationTokens, status);
        this.allowedEmailType = allowedEmailType;
        this.credentialSource = credentialSource;
        this.emailProviderConnectionId = emailProviderConnectionId;
    }

    /**
     * Convenience constructor accepting raw string for domainName.
     */
    public Domain(UUID id, User_command user, String domainName, List<String> verificationTokens, DomainStatus status) {
        this(id, user, DomainName.of(domainName), verificationTokens, status);
    }

    public void updateReadiness() {
        if (!spfVerified || !dkimVerified || !dmarcVerified) {
            this.sendReadiness = DomainSendReadiness.PARTIALLY_VERIFIED;
        } else if (this.warmingStatus == WarmingStatus.PAUSED) {
            this.sendReadiness = DomainSendReadiness.SUSPENDED;
        } else {
            this.sendReadiness = DomainSendReadiness.READY;
        }
        
        if (this.status == DomainStatus.PENDING) {
            this.sendReadiness = DomainSendReadiness.UNVERIFIED;
        }
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User_command getUser() { return user; }
    public void setUser(User_command user) { this.user = user; }

    public DomainName getDomainNameVO() { return domainName; }
    public String getDomainName() { return domainName != null ? domainName.value() : null; }
    public void setDomainName(DomainName domainName) { this.domainName = domainName; }
    public void setDomainName(String domainName) { this.domainName = DomainName.of(domainName); }

    public List<String> getVerificationTokens() { return verificationTokens; }
    public void setVerificationTokens(List<String> verificationTokens) { this.verificationTokens = verificationTokens; }

    public DomainStatus getStatus() { return status; }
    public void setStatus(DomainStatus status) { this.status = status; this.updateReadiness(); }

    public boolean isSpfVerified() { return spfVerified; }
    public void setSpfVerified(boolean spfVerified) { this.spfVerified = spfVerified; this.updateReadiness(); }

    public boolean isDkimVerified() { return dkimVerified; }
    public void setDkimVerified(boolean dkimVerified) { this.dkimVerified = dkimVerified; this.updateReadiness(); }

    public boolean isDmarcVerified() { return dmarcVerified; }
    public void setDmarcVerified(boolean dmarcVerified) { this.dmarcVerified = dmarcVerified; this.updateReadiness(); }

    public int getDailySendCap() { return dailySendCap; }
    public void setDailySendCap(int dailySendCap) { this.dailySendCap = dailySendCap; }

    public WarmingStatus getWarmingStatus() { return warmingStatus; }
    public void setWarmingStatus(WarmingStatus warmingStatus) { this.warmingStatus = warmingStatus; this.updateReadiness(); }

    public DomainSendReadiness getSendReadiness() { return sendReadiness; }
    public void setSendReadiness(DomainSendReadiness sendReadiness) { this.sendReadiness = sendReadiness; }

    public AllowedEmailType getAllowedEmailType() { return allowedEmailType; }
    public void setAllowedEmailType(AllowedEmailType allowedEmailType) { this.allowedEmailType = allowedEmailType; }

    public CredentialSource getCredentialSource() { return credentialSource; }
    public void setCredentialSource(CredentialSource credentialSource) { this.credentialSource = credentialSource; }

    public UUID getEmailProviderConnectionId() { return emailProviderConnectionId; }
    public void setEmailProviderConnectionId(UUID emailProviderConnectionId) { this.emailProviderConnectionId = emailProviderConnectionId; }

    public boolean isTrackingEnabled() { return trackingEnabled; }
    public void setTrackingEnabled(boolean trackingEnabled) { this.trackingEnabled = trackingEnabled; }

    public String getUnsubscribeLogoUrl() { return unsubscribeLogoUrl; }
    public void setUnsubscribeLogoUrl(String unsubscribeLogoUrl) { this.unsubscribeLogoUrl = unsubscribeLogoUrl; }

    public String getUnsubscribePrimaryColor() { return unsubscribePrimaryColor; }
    public void setUnsubscribePrimaryColor(String unsubscribePrimaryColor) { this.unsubscribePrimaryColor = unsubscribePrimaryColor; }

    public String getUnsubscribeCopy() { return unsubscribeCopy; }
    public void setUnsubscribeCopy(String unsubscribeCopy) { this.unsubscribeCopy = unsubscribeCopy; }

    public String getBimiLogoUrl() { return bimiLogoUrl; }
    public void setBimiLogoUrl(String bimiLogoUrl) { this.bimiLogoUrl = bimiLogoUrl; }

    public String getBimiVmcUrl() { return bimiVmcUrl; }
    public void setBimiVmcUrl(String bimiVmcUrl) { this.bimiVmcUrl = bimiVmcUrl; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
}
