package com.crescendo.user.user_command.user_session;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

		@Query("""
				select s from UserSession s
				where s.refreshTokenHash = :hash
					and s.revokedAt is null
					and s.expiresAt > :now
		""")
		Optional<UserSession> findActiveByHash(String hash, Instant now);

		@Query("""
				select s from UserSession s
				where s.user.id = :userId
					and s.revokedAt is null
					and s.expiresAt > :now
		""")
		List<UserSession> findAllActiveByUserId(UUID userId, Instant now);

		Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);

	/**
	 * Deletes all sessions (active, revoked, and expired) for a given user.
	 * Used during account deletion to avoid loading all sessions into memory.
	 */
	void deleteAllByUser_Id(UUID userId);
}
