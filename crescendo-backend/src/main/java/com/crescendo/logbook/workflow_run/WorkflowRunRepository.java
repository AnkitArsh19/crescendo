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

    Optional<WorkflowRun> findByResumeTokenAndStatus(String resumeToken, WorkflowRunStatus status);

    List<WorkflowRun> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<WorkflowRun> findAllByUserId(UUID userId, Pageable pageable);

    List<WorkflowRun> findAllByWorkflowIdAndUserIdAndStatus(
            UUID workflowId, UUID userId, WorkflowRunStatus status);

    long countByWorkflowIdAndUserId(UUID workflowId, UUID userId);

    long countByWorkflowIdAndUserIdAndStatus(UUID workflowId, UUID userId, WorkflowRunStatus status);

    long countByUserId(UUID userId);

    long countByUserIdAndStatus(UUID userId, WorkflowRunStatus status);


    @org.springframework.data.jpa.repository.Query("SELECT r FROM WorkflowRun r WHERE r.status = :status AND r.createdAt < :threshold")
    List<WorkflowRun> findByStatusAndCreatedAtBefore(
            @org.springframework.data.repository.query.Param("status") WorkflowRunStatus status,
            @org.springframework.data.repository.query.Param("threshold") java.time.Instant threshold);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM WorkflowRun r WHERE r.status = 'SUSPENDED' AND r.resumeAt <= :now")
    List<WorkflowRun> findReadyToResumeWorkflows(@org.springframework.data.repository.query.Param("now") java.time.Instant now);

    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM workflow_run WHERE \"userId\" = :userId AND search_vector @@ plainto_tsquery('english', :query) ORDER BY created_at DESC LIMIT 50", nativeQuery = true)
    List<WorkflowRun> searchByQuery(@org.springframework.data.repository.query.Param("userId") UUID userId, @org.springframework.data.repository.query.Param("query") String query);
}
