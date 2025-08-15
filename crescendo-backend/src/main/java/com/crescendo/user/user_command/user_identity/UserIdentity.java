package com.crescendo.user.user_command.user_identity;

import com.crescendo.enums.AuthProvider;
import com.crescendo.user.user_command.User_command;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Links an external identity provider account (Google, GitHub, etc.) to a local user.
 */
@Entity
/// Indexes tell that when creating table create indexes for the given columns.
/// Here the selection process improves.
/// The index column is the name given, and it creates index from the column list given
@Table(name = "user_identity",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_user_identity_provider_uid", columnNames = {"provider", "provider_user_id"})
		},
		indexes = {
				@Index(name = "idx_user_identity_user_id", columnList = "user_id"),
				@Index(name = "idx_user_identity_provider_uid", columnList = "provider,provider_user_id")
		}
)
public class UserIdentity {

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
	@JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_identity_user"))
	private User_command user;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider", nullable = false)
	private AuthProvider provider;

	@Column(name = "provider_user_id", nullable = false)
	private String providerUserId;

	@Column(name = "email", nullable = true)
	private String email;

	@CreationTimestamp
	@Column(name = "linked_at", nullable = false, updatable = false)
	private Instant linkedAt;

	public UserIdentity() {
	}

	public UserIdentity(UUID id, User_command user, AuthProvider provider, String providerUserId, String email) {
		this.id = id;
		this.user = user;
		this.provider = provider;
		this.providerUserId = providerUserId;
		this.email = email;
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

	public AuthProvider getProvider() {
		return provider;
	}

	public void setProvider(AuthProvider provider) {
		this.provider = provider;
	}

	public String getProviderUserId() {
		return providerUserId;
	}

	public void setProviderUserId(String providerUserId) {
		this.providerUserId = providerUserId;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Instant getLinkedAt() {
		return linkedAt;
	}
}
