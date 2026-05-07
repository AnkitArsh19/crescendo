package com.crescendo.security.mfa;

import com.crescendo.user.user_command.User_command;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_mfa_setting", indexes = {
        @Index(name = "idx_mfa_user_id", columnList = "user_id"),
        @Index(name = "idx_mfa_enabled", columnList = "enabled")
})
public class UserMFASetting {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, foreignKey = @ForeignKey(name = "fk_mfa_user"))
    private User_command user;

    @Column(name = "secret", nullable = false, length = 64)
    private String secret; // Base32 secret

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "verified", nullable = false)
    private boolean verified; // secret confirmed by user via code

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserMFASetting() {}

    public UserMFASetting(UUID id, User_command user, String secret) {
        this.id = id;
        this.user = user;
        this.secret = secret;
        this.enabled = false;
        this.verified = false;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User_command getUser() { return user; }
    public void setUser(User_command user) { this.user = user; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
