package com.crescendo.auth.token.password;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /// Looks up a reset token by its SHA-256 hash — used to validate a token emailed to the user.
    /// We never store raw tokens, only hashes, so this is the only lookup path.
    java.util.Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}
