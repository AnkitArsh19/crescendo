package com.crescendo.workflow.workflow_command;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface Workflow_commandRepository extends JpaRepository<Workflow_command, UUID> {

    /// Counts non-deleted workflows owned by a registered user.
    /// Used by AccessControlService to enforce per-tier workflow limits.
    long countByUser_IdAndDeletedAtIsNull(UUID userId);

    /// Counts non-deleted workflows owned by a guest session.
    /// Guests are identified by session ID instead of a user account.
    long countByGuestSessionId_ValueAndDeletedAtIsNull(String guestSessionId);

    /// Finds a non-deleted workflow by ID (regardless of owner).
    Optional<Workflow_command> findByIdAndDeletedAtIsNull(UUID id);

    /// All non-deleted workflows for a registered user, newest first.
    List<Workflow_command> findAllByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);

    /// All non-deleted workflows for a guest session, newest first.
    List<Workflow_command> findAllByGuestSessionId_ValueAndDeletedAtIsNullOrderByCreatedAtDesc(String guestSessionId);
}
