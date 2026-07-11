package com.crescendo.workflow.workflow_command;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface WorkflowEdge_commandRepository extends JpaRepository<WorkflowEdge_command, UUID> {

    List<WorkflowEdge_command> findByWorkflowId(UUID workflowId);

    @Transactional
    @Modifying
    @Query("DELETE FROM WorkflowEdge_command e WHERE e.workflowId = :workflowId")
    void deleteByWorkflowId(@Param("workflowId") UUID workflowId);

    @Transactional
    @Modifying
    @Query("DELETE FROM WorkflowEdge_command e WHERE e.sourceStepId = :stepId OR e.targetStepId = :stepId")
    void deleteBySourceStepIdOrTargetStepId(@Param("stepId") UUID stepId);
}
