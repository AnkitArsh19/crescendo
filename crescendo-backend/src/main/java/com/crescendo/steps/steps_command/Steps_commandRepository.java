package com.crescendo.steps.steps_command;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface Steps_commandRepository extends JpaRepository<Steps_command, UUID> {

    /// Non-deleted steps for a workflow, ordered by step_order ascending.
    @Query("SELECT s FROM Steps_command s WHERE s.workflow.id = :workflowId AND s.deletedAt IS NULL ORDER BY s.order.value ASC")
    List<Steps_command> findActiveByWorkflowIdOrdered(UUID workflowId);

    /// Count of non-deleted steps for a workflow (used for limit enforcement).
    long countByWorkflow_IdAndDeletedAtIsNull(UUID workflowId);

    /// Find a non-deleted step by ID.
    Optional<Steps_command> findByIdAndDeletedAtIsNull(UUID id);

    /// Soft-delete all steps for a workflow (used when soft-deleting a workflow).
    @Query("SELECT s FROM Steps_command s WHERE s.workflow.id = :workflowId AND s.deletedAt IS NULL")
    List<Steps_command> findActiveByWorkflowId(UUID workflowId);
}
