package com.crescendo.emailservice.outboundwebhook;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookDeliveryLogRepository extends JpaRepository<WebhookDeliveryLog, UUID> {

    Page<WebhookDeliveryLog> findBySubscriptionIdOrderByCreatedAtDesc(UUID subscriptionId, Pageable pageable);

    /**
     * Claim pending deliveries for dispatch using SELECT … FOR UPDATE SKIP LOCKED.
     * Each row is locked exclusively by the first worker that claims it; concurrent
     * workers skip locked rows and claim different ones, preventing duplicate delivery.
     *
     * @param now     Current instant — only rows with nextRetryAt &lt;= now are eligible.
     * @param pageable Limits the batch size (e.g., max 50 rows per scheduler tick).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0")) // 0 = SKIP LOCKED
    @Query("SELECT d FROM WebhookDeliveryLog d WHERE d.status = 'PENDING' AND d.nextRetryAt <= :now ORDER BY d.nextRetryAt ASC")
    List<WebhookDeliveryLog> claimPendingForDispatch(@Param("now") Instant now, Pageable pageable);
}

