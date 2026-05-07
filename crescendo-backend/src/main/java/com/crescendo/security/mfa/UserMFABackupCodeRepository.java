package com.crescendo.security.mfa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserMFABackupCodeRepository extends JpaRepository<UserMFABackupCode, UUID> {

    /// Returns only the unused backup codes for a user — used to count remaining codes.
    List<UserMFABackupCode> findAllByUser_IdAndUsedAtIsNull(UUID userId);
    /// Returns all backup codes for a user (used + unused) — used for admin/audit views.
    List<UserMFABackupCode> findAllByUser_Id(UUID userId);
    /// Looks up an unused backup code by its hash — the core lookup during backup code authentication.
    /// The triple condition (userId + hash + usedAt IS NULL) ensures a code can only be matched once.
    Optional<UserMFABackupCode> findByUser_IdAndCodeHashAndUsedAtIsNull(UUID userId, String hash);

    /// Bulk-deletes all backup codes for a user before regenerating a fresh set.
    /// @Modifying is required for any DELETE/UPDATE @Query — without it Spring throws an exception.
    /// An explicit JPQL query is used (rather than a derived deleteBy) to issue a single DELETE
    /// statement instead of N individual deletes (one per entity loaded and then removed).
    @Modifying
    @Query("delete from UserMFABackupCode c where c.user.id = :userId")
    void deleteAllByUserId(UUID userId);
}
