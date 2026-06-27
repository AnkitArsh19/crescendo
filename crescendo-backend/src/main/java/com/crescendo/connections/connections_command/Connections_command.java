package com.crescendo.connections.connections_command;

import com.crescendo.enums.ConnectionStatus;
import com.crescendo.shared.domain.valueobject.AppKey;
import com.crescendo.user.user_command.User_command;
import jakarta.persistence.*;
import jakarta.persistence.Index;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
/// Indexes tell that when creating table create indexes for the given columns.
/// Here the selection process improves. The index column is the name given, and
/// it creates index from the column list given
@Table(name = "connections_command", indexes = {
        @Index(name = "idx_connection_user", columnList = "userId"),
        @Index(name = "idx_connection_status", columnList = "status")
})
public class Connections_command {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "userId", referencedColumnName = "id", nullable = false, foreignKey = @ForeignKey(name = "fk_connection_user"))
    private User_command user;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "appKey", nullable = false, length = 100))
    private AppKey appKey;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "credentials", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> credentials;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ConnectionStatus status;

    /**
     * Indicates which master key version was used to encrypt {@code credentials}.
     * Defaults to 1. When the master key is rotated, re-encrypted entries will get
     * the new version number. This allows safe migration without a big-bang
     * re-encryption.
     */
    @Column(name = "key_version", nullable = false, columnDefinition = "integer default 1")
    private int keyVersion = 1;

    /**
     * Space/comma-separated OAuth scopes that were actually granted by the user
     * at the time the connection was created.
     * Populated from the {@code scope} field in the token response (most providers
     * return it).
     * {@code null} for API-key connections and for OAuth connections where the
     * provider
     * did not return a scope string.
     * Used by the frontend to grey out actions that require scopes the user did not
     * grant.
     */
    @Column(name = "granted_scopes", columnDefinition = "TEXT")
    private String grantedScopes;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updatedAt", nullable = false)
    private Instant updatedAt;

    public Connections_command() {
    }

    public Connections_command(UUID id, User_command user, AppKey appKey, String name, Map<String, Object> credentials,
            ConnectionStatus status) {
        this.id = id;
        this.user = user;
        this.appKey = appKey;
        this.name = name;
        this.credentials = credentials;
        this.status = status;
    }

    /**
     * Convenience constructor accepting raw string for appKey.
     */
    public Connections_command(UUID id, User_command user, String appKey, String name, Map<String, Object> credentials,
            ConnectionStatus status) {
        this(id, user, AppKey.of(appKey), name, credentials, status);
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

    public AppKey getAppKeyVO() {
        return appKey;
    }

    /**
     * Returns raw app key string for compatibility.
     */
    public String getAppKey() {
        return appKey != null ? appKey.value() : null;
    }

    public void setAppKey(AppKey appKey) {
        this.appKey = appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = AppKey.of(appKey);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getCredentials() {
        return credentials;
    }

    public void setCredentials(Map<String, Object> credentials) {
        this.credentials = credentials;
    }

    public ConnectionStatus getStatus() {
        return status;
    }

    public void setStatus(ConnectionStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getKeyVersion() {
        return keyVersion;
    }

    public void setKeyVersion(int keyVersion) {
        this.keyVersion = keyVersion;
    }

    public String getGrantedScopes() {
        return grantedScopes;
    }

    public void setGrantedScopes(String grantedScopes) {
        this.grantedScopes = grantedScopes;
    }
}
