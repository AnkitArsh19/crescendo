package com.crescendo.user.user_command.user_credential;

import com.crescendo.user.user_command.User_command;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Stores credentials only for users that have a local (password-based) login.
 * Users that authenticate exclusively via OAuth will have no data here.
 */
@Entity
@Table(name = "user_credential",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_user_credential_user_id", columnNames = {"user_id"})
		},
		indexes = {
				@Index(name = "idx_user_credential_user_id", columnList = "user_id")
		}
)
public class UserCredential {

	@Id
	@Column(name = "id", nullable = false)
	private UUID id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_credential_user"))
	private User_command user;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public UserCredential() {
	}

	public UserCredential(UUID id, User_command user, String passwordHash) {
		this.id = id;
		this.user = user;
		this.passwordHash = passwordHash;
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

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
