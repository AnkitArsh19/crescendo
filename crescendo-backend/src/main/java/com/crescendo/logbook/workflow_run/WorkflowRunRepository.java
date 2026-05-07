package com.crescendo.logbook.workflow_run;

import com.crescendo.enums.WorkflowRunStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowRunRepository extends JpaRepository<WorkflowRun, UUID> {

    List<WorkflowRun> findAllByWorkflowIdAndUserIdOrderByCreatedAtDesc(UUID workflowId, UUID userId);

    Page<WorkflowRun> findAllByWorkflowIdAndUserId(UUID workflowId, UUID userId, Pageable pageable);

    Optional<WorkflowRun> findByIdAndUserId(UUID id, UUID userId);

    List<WorkflowRun> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<WorkflowRun> findAllByUserId(UUID userId, Pageable pageable);

    List<WorkflowRun> findAllByWorkflowIdAndUserIdAndStatus(
            UUID workflowId, UUID userId, WorkflowRunStatus status);

    long countByWorkflowIdAndUserId(UUID workflowId, UUID userId);

    long countByWorkflowIdAndUserIdAndStatus(UUID workflowId, UUID userId, WorkflowRunStatus status);
}
