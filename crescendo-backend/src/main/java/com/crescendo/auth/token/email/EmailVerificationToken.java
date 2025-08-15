package com.crescendo.auth.token.email;

import com.crescendo.user.user_command.User_command;
import jakarta.persistence.*;
import jakarta.persistence.Index;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Stores single-use email verification tokens (hashed) for users.
 */
@Entity
/// Indexes tell that when creating table create indexes for the given columns.
/// Here the selection process improves.
/// The index column is the name given, and it creates index from the column list given
@Table(name = "email_verification_token",
		indexes = {
				@Index(name = "idx_email_verification_user_id", columnList = "user_id"),
				@Index(name = "idx_email_verification_expires_at", columnList = "expires_at")
		}
)
public class EmailVerificationToken {

	@Id
	@Column(name = "id", nullable = false)
	private UUID id;

    /// ManyToOne is used to map many entities to one entity
    /// FetchType.LAZY means that the referenced entity will not be loaded from the database until we actually access it
    /// optional=false means that the relationship is mandatory
    /// JoinColumn tells how relationship is mapped.
    /// Referenced column name is the name of the column of the foreign table.
    /// Foreign Key is used to explicitly name the foreign key constraint
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, foreignKey = @ForeignKey(name = "fk_email_verification_user"))
	private User_command user;

	/**
	 * Secure hash of the raw token sent to the user (never store raw token).
	 */
	@Column(name = "token_hash", nullable = false)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "consumed_at")
	private Instant consumedAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public EmailVerificationToken() {}

	public EmailVerificationToken(UUID id, User_command user, String tokenHash, Instant expiresAt) {
		this.id = id;
		this.user = user;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	public UUID getId() { return id; }
	public void setId(UUID id) { this.id = id; }
	public User_command getUser() { return user; }
	public void setUser(User_command user) { this.user = user; }
	public String getTokenHash() { return tokenHash; }
	public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
	public Instant getExpiresAt() { return expiresAt; }
	public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
	public Instant getConsumedAt() { return consumedAt; }
	public void setConsumedAt(Instant consumedAt) { this.consumedAt = consumedAt; }
	public Instant getCreatedAt() { return createdAt; }
}
