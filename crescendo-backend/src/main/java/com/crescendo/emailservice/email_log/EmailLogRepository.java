package com.crescendo.emailservice.email_log;

import com.crescendo.enums.EmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, UUID> {

    List<EmailLog> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<EmailLog> findByIdAndUserId(UUID id, UUID userId);

    List<EmailLog> findByAppKeyIdOrderByCreatedAtDesc(UUID appKeyId);

    Optional<EmailLog> findByProviderMessageId(String providerMessageId);

    long countByUserIdAndStatus(UUID userId, EmailStatus status);

    long countByUserId(UUID userId);

    @Query("SELECT e.status, COUNT(e) FROM EmailLog e WHERE e.userId = :userId GROUP BY e.status")
    List<Object[]> countGroupedByStatus(@Param("userId") UUID userId);

    @Query(value = "SELECT CAST(created_at AS DATE) AS day, status, COUNT(*) AS cnt " +
            "FROM email_log WHERE \"userId\" = :userId AND created_at >= :since " +
            "GROUP BY CAST(created_at AS DATE), status ORDER BY day",
            nativeQuery = true)
    List<Object[]> dailyCountsSince(@Param("userId") UUID userId, @Param("since") Instant since);

    @Query("SELECT SUM(e.openCount) FROM EmailLog e WHERE e.userId = :userId")
    Long totalOpenCount(@Param("userId") UUID userId);

    @Query("SELECT SUM(e.clickCount) FROM EmailLog e WHERE e.userId = :userId")
    Long totalClickCount(@Param("userId") UUID userId);
}
