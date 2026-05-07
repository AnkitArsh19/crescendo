package com.crescendo.logbook.step_run;

import com.crescendo.enums.StepRunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StepRunRepository extends JpaRepository<StepRun, UUID> {

    List<StepRun> findAllByWorkflowRunIdOrderByCreatedAtAsc(UUID workflowRunId);

    List<StepRun> findAllByWorkflowRunIdAndStatus(UUID workflowRunId, StepRunStatus status);

    long countByWorkflowRunId(UUID workflowRunId);

    long countByWorkflowRunIdAndStatus(UUID workflowRunId, StepRunStatus status);
}
