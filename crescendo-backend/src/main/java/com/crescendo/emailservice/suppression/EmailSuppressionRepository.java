package com.crescendo.emailservice.suppression;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailSuppressionRepository extends JpaRepository<EmailSuppression, UUID> {

    boolean existsByUserIdAndNormalizedEmail(UUID userId, String normalizedEmail);

    List<EmailSuppression> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<EmailSuppression> findByUserIdAndNormalizedEmail(UUID userId, String normalizedEmail);
}
