package com.crescendo.auth.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DesktopHandoffCodeRepository extends JpaRepository<DesktopHandoffCode, UUID> {

    /** Look up a code by the SHA-256 hash of the raw value presented by the desktop app. */
    Optional<DesktopHandoffCode> findByCodeHash(String codeHash);

    /**
     * Bulk-delete all expired codes. Called periodically (e.g. via @Scheduled) to
     * prevent unbounded table growth. Codes expire in 60 seconds so this can run infrequently.
     */
    @Modifying
    @Query("delete from DesktopHandoffCode c where c.expiresAt < :now")
    void deleteAllExpiredBefore(Instant now);
}
