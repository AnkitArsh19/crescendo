package com.crescendo.user.user_query;

import com.crescendo.enums.UserRole;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "user_query")
public class User_query {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "email_id", nullable = false)
    private String emailId;

    @Column(name = "username", nullable = false)
    private String userName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "providers", columnDefinition = "json", nullable = false)
    private List<String> providers;

    @Column(name = "has_local_credential", nullable = false)
    private boolean hasLocalCredential;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updatedAt", nullable = false)
    private Instant updatedAt;

    public User_query() {
    }

    public User_query(UUID id, String userName, UserRole role, List<String> providers, boolean hasLocalCredential, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userName = userName;
        this.role = role;
        this.providers = providers;
        this.hasLocalCredential = hasLocalCredential;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getUserName() {
        return userName;
    }

    public UserRole getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<String> getProviders() { return providers; }

    public boolean isHasLocalCredential() { return hasLocalCredential; }
}
