package com.crescendo.admin;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Stores whitelisted admin emails.
 * Users registering with these emails are automatically promoted to ADMIN.
 */
@Entity
@Table(name = "admin_email", indexes = {
        @Index(name = "idx_admin_email", columnList = "email", unique = true)
})
public class AdminEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "added_by")
    private String addedBy;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    public AdminEmail() {}

    public AdminEmail(String email, String addedBy) {
        this.email = email.trim().toLowerCase();
        this.addedBy = addedBy;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getAddedBy() { return addedBy; }
    public Instant getAddedAt() { return addedAt; }
}
