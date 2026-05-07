package com.crescendo.security.mfa;

import com.crescendo.user.user_command.User_command;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_mfa_backup_code", indexes = {
        @Index(name = "idx_mfa_backup_user", columnList = "user_id"),
        @Index(name = "idx_mfa_backup_hash", columnList = "code_hash")
})
public class UserMFABackupCode {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, foreignKey = @ForeignKey(name = "fk_mfa_backup_user"))
    private User_command user;

    /// SHA-256 hash of the raw backup code — never stored in plain text.
    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    /// Null = code has not been used. Non-null = code was consumed at this timestamp.
    /// A code can only be used once — queried with 'usedAt IS NULL' to find available codes.
    @Column(name = "used_at")
    private Instant usedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UserMFABackupCode() {}

    public UserMFABackupCode(UUID id, User_command user, String codeHash) {
        this.id = id;
        this.user = user;
        this.codeHash = codeHash;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User_command getUser() { return user; }
    public void setUser(User_command user) { this.user = user; }
    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }
    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
