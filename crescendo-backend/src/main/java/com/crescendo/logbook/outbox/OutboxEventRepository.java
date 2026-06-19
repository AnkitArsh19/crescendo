package com.crescendo.logbook.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Fetches a batch of unpublished events that haven't exceeded the max attempt
     * threshold, ordered oldest-first, with a pessimistic write lock to prevent
     * concurrent instances from processing the same events.
     *
     * @param maxAttempts events with attemptCount >= this value are excluded
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OutboxEvent o WHERE o.published = false AND o.attemptCount < :maxAttempts ORDER BY o.createdAt ASC")
    List<OutboxEvent> findBatchForUpdate(@Param("maxAttempts") int maxAttempts, Pageable pageable);

    /**
     * Deletes all published (or permanently failed) events created before the given threshold.
     * Used by the cleanup scheduler to prevent unbounded table growth.
     *
     * @return the number of rows deleted
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent o WHERE o.published = true AND o.createdAt < :threshold")
    int deletePublishedBefore(@Param("threshold") Instant threshold);
}