package com.crescendo.workflow.workflow_query;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface WorkflowEdge_queryRepository extends JpaRepository<WorkflowEdge_query, UUID> {

    List<WorkflowEdge_query> findByWorkflowId(UUID workflowId);

    @Transactional(transactionManager = "queryTransactionManager")
    @Modifying
    @Query("DELETE FROM WorkflowEdge_query e WHERE e.workflowId = :workflowId")
    void deleteByWorkflowId(@Param("workflowId") UUID workflowId);

    @Transactional(transactionManager = "queryTransactionManager")
    @Modifying
    @Query("DELETE FROM WorkflowEdge_query e WHERE e.sourceStepId = :stepId OR e.targetStepId = :stepId")
    void deleteBySourceStepIdOrTargetStepId(@Param("stepId") UUID stepId);
}
