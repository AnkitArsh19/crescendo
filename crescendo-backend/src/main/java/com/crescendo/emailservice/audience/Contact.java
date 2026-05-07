package com.crescendo.emailservice.audience;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * A contact in the user's audience — a recipient who can be targeted by broadcasts.
 * Each (userId, email) pair is unique so the same address cannot be added twice.
 *
 * Contacts are per-user (single-tenant). The subscribed flag controls whether
 * they receive broadcasts — unsubscribed contacts are skipped during fan-out.
 */
@Entity
@Table(name = "email_contact",
        indexes = {
                @Index(name = "idx_contact_user", columnList = "userId"),
                @Index(name = "idx_contact_user_email", columnList = "userId,email", unique = true)
        })
public class Contact {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

    @Column(nullable = false)
    private boolean subscribed = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    protected Contact() {}

    public Contact(UUID id, UUID userId, String email, String firstName, String lastName) {
        this.id = id;
        this.userId = userId;
        this.email = email.toLowerCase().trim();
        this.firstName = firstName;
        this.lastName = lastName;
        this.subscribed = true;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email.toLowerCase().trim(); }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public boolean isSubscribed() { return subscribed; }
    public void setSubscribed(boolean subscribed) { this.subscribed = subscribed; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
