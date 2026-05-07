package com.crescendo.workflow.workflow_command;

import com.crescendo.shared.domain.valueobject.GuestSessionId;
import com.crescendo.user.user_command.User_command;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
/// Indexes tell that when creating table create indexes for the given columns.
/// Here the selection process improves.
/// The index column is the name given, and it creates index from the column list given
@Table(name = "workflow_command",
    indexes = {
        @Index(name = "idx_workflow_user", columnList = "userId"),
        @Index(name = "idx_workflow_active", columnList = "isActive")
    })
public class Workflow_command {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    /// ManyToOne is used to map many entities to one entity
    /// FetchType.LAZY means that the referenced entity will not be loaded from the database until we actually access it
    /// optional=false means that the relationship is mandatory
    /// JoinColumn tells how relationship is mapped.
    /// Referenced column name is the name of the column of the foreign table.
    /// Foreign Key is used to explicitly name the foreign key constraint
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "userId", referencedColumnName = "id", nullable = true, foreignKey = @ForeignKey(name = "fk_workflow_user"))
    private User_command user;

    /**
     * For guest users who don't have an account yet.
     * Allows trying the app without login.
     */
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "guestSessionId", length = 100))
    private GuestSessionId guestSessionId;

    @Column(name = "deletedAt")
    private Instant deletedAt;

    @Column(name = "isActive", nullable = false)
    private boolean isActive;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updatedAt", nullable = false)
    private Instant updatedAt;

    public Workflow_command() {
    }

    public Workflow_command(UUID id, String name, String description, User_command user, boolean isActive) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.user = user;
        this.isActive = isActive;
    }

    public Workflow_command(UUID id, String name, String description, GuestSessionId guestSessionId, boolean isActive) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.guestSessionId = guestSessionId;
        this.isActive = isActive;
    }

    /**
     * Convenience constructor for guest workflow with raw string session ID.
     */
    public Workflow_command(UUID id, String name, String description, String guestSessionIdStr, boolean isActive) {
        this(id, name, description, GuestSessionId.of(guestSessionIdStr), isActive);
    }

    /**
     * Check if this is a guest workflow (no registered user).
     */
    public boolean isGuestWorkflow() {
        return user == null && guestSessionId != null;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User_command getUser() {
        return user;
    }
    public void setUser(User_command user) {
        this.user = user;
    }

    public GuestSessionId getGuestSessionIdVO() {
        return guestSessionId;
    }

    /**
     * Returns raw guest session ID string for compatibility.
     */
    public String getGuestSessionId() {
        return guestSessionId != null ? guestSessionId.value() : null;
    }
    public void setGuestSessionId(GuestSessionId guestSessionId) {
        this.guestSessionId = guestSessionId;
    }
    public void setGuestSessionId(String guestSessionId) {
        this.guestSessionId = guestSessionId != null ? GuestSessionId.of(guestSessionId) : null;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
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
}
