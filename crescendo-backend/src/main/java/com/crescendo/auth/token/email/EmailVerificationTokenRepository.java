package com.crescendo.auth.token.email;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    /// Looks up an email verification token by its SHA-256 hash — used to validate the link clicked by the user.
    java.util.Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
}
