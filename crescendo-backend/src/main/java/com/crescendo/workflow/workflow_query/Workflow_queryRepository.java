package com.crescendo.workflow.workflow_query;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface Workflow_queryRepository extends JpaRepository<Workflow_query, UUID> {

    /// Read-side count of workflows for a registered user (complements the command-side count).
    long countByUserId(UUID userId);

    /// Read-side count of workflows for a guest session.
    long countByGuestSessionId(String guestSessionId);

    /// All workflows for a registered user, newest first.
    List<Workflow_query> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    /// All workflows for a guest session, newest first.
    List<Workflow_query> findAllByGuestSessionIdOrderByCreatedAtDesc(String guestSessionId);

    /// Single workflow owned by a registered user.
    Optional<Workflow_query> findByIdAndUserId(UUID id, UUID userId);

    /// Single workflow owned by a guest session.
    Optional<Workflow_query> findByIdAndGuestSessionId(UUID id, String guestSessionId);

    /// Batch lookup by IDs — used for shared workflow previews.
    List<Workflow_query> findAllByIdIn(List<UUID> ids);

    /// Direct update of step_count — avoids cross-transaction issues in CQRS.
    @Modifying
    @Query("UPDATE Workflow_query w SET w.step_count = :count WHERE w.id = :id")
    void updateStepCount(@Param("id") UUID id, @Param("count") int count);

    /// Direct update of isActive — avoids cross-transaction issues in CQRS.
    @Modifying
    @Query("UPDATE Workflow_query w SET w.isActive = :active WHERE w.id = :id")
    void updateIsActive(@Param("id") UUID id, @Param("active") boolean active);
}
