package com.crescendo.workflow.workflow_query;

import com.crescendo.steps.steps_query.Steps_queryService;
import com.crescendo.workflow.WorkflowDto;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Read-side service for workflow queries.
 *
 * List operations use the query database (Workflow_query, Steps_query) which holds
 * denormalized, read-optimized projections synced by Workflow_commandService.
 *
 * Detail operations use the command-side Steps_command repo for step fields
 * not present in the query model (e.g. actionKey). This will move fully to
 * query-side reads once the query projections include all needed fields.
 */
@Service
public class Workflow_queryService {

    private final Workflow_queryRepository workflowQueryRepo;
    private final Steps_queryService stepsQueryService;
    private final WorkflowEdge_queryRepository edgeQueryRepo;

    public Workflow_queryService(Workflow_queryRepository workflowQueryRepo,
                                 Steps_queryService stepsQueryService,
                                 WorkflowEdge_queryRepository edgeQueryRepo) {
        this.workflowQueryRepo = workflowQueryRepo;
        this.stepsQueryService = stepsQueryService;
        this.edgeQueryRepo = edgeQueryRepo;
    }

    // WORKFLOW LISTING

    /**
     * Lists all workflows for a registered user (summary only — no step detail).
     * Returns newest first.
     *
     * Cached per userId with a short TTL (60 s). Evicted on workflow create/delete
     * by {@code WorkflowEventListener} so the list reflects changes promptly.
     */
    @Cacheable(value = "workflowLists", key = "#userId")
    public List<WorkflowDto.WorkflowSummaryResponse> listWorkflows(UUID userId) {
        return workflowQueryRepo.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    /**
     * Lists workflows for a registered user with pagination and optional status filter.
     */
    public WorkflowDto.WorkflowListResponse listWorkflowsPaginated(UUID userId, String status, String cursor, int limit) {
        java.time.Instant cursorTime = null;
        UUID cursorId = null;
        
        if (cursor != null && !cursor.isBlank()) {
            try {
                String decoded = new String(java.util.Base64.getUrlDecoder().decode(cursor));
                String[] parts = decoded.split("_");
                if (parts.length == 2) {
                    cursorTime = java.time.Instant.ofEpochMilli(Long.parseLong(parts[0]));
                    cursorId = UUID.fromString(parts[1]);
                }
            } catch (Exception e) {
                // Ignore malformed cursor, fall back to first page
            }
        }
        
        List<Workflow_query> results = workflowQueryRepo.findPaginatedByUserId(
                userId, status, cursorTime, cursorId, limit);
        
        String nextCursor = null;
        if (results.size() == limit && !results.isEmpty()) {
            Workflow_query last = results.get(results.size() - 1);
            String rawCursor = last.getCreatedAt().toEpochMilli() + "_" + last.getId();
            nextCursor = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(rawCursor.getBytes());
        }
        
        List<WorkflowDto.WorkflowSummaryResponse> mapped = results.stream().map(this::toSummary).toList();
        return new WorkflowDto.WorkflowListResponse(mapped, nextCursor);
    }

    /**
     * Lists all workflows for a guest session (summary only).
     * Returns newest first.
     */
    public List<WorkflowDto.WorkflowSummaryResponse> listGuestWorkflows(String guestSessionId) {
        return workflowQueryRepo.findAllByGuestSessionIdOrderByCreatedAtDesc(guestSessionId)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    // WORKFLOW DETAIL
    /**
     * Returns a single workflow with all its steps for a registered user.
     * Steps are ordered by their step_order ascending.
     * Uses command-side repo for step detail (actionKey not in query model).
     */
    @Cacheable(value = "workflows", key = "'detail:v2:' + #userId + ':' + #workflowId")
    public WorkflowDto.WorkflowDetailResponse getWorkflowDetail(UUID userId, UUID workflowId) {
        Workflow_query workflow = workflowQueryRepo.findByIdAndUserId(workflowId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));

        List<WorkflowDto.StepResponse> steps = stepsQueryService.loadSteps(workflowId);
        List<WorkflowDto.EdgeResponse> edges = loadEdges(workflowId);
        return toDetail(workflow, steps, edges);
    }

    /**
     * Returns a single workflow with all its steps for a guest session.
     */
    @Cacheable(value = "workflows", key = "'guest-detail:v2:' + #guestSessionId + ':' + #workflowId")
    public WorkflowDto.WorkflowDetailResponse getGuestWorkflowDetail(String guestSessionId, UUID workflowId) {
        Workflow_query workflow = workflowQueryRepo.findByIdAndGuestSessionId(workflowId, guestSessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));

        List<WorkflowDto.StepResponse> steps = stepsQueryService.loadSteps(workflowId);
        List<WorkflowDto.EdgeResponse> edges = loadEdges(workflowId);
        return toDetail(workflow, steps, edges);
    }

    // DTO MAPPERS

    private WorkflowDto.WorkflowSummaryResponse toSummary(Workflow_query w) {
        return new WorkflowDto.WorkflowSummaryResponse(
                w.getId().toString(),
                w.getName(),
                w.getDescription(),
                w.isActive(),
                w.getStatus().name(),
                w.getStep_count(),
                w.getVersion(),
                w.getCreatedAt(),
                w.getUpdatedAt(),
                w.getLastRunAt()
        );
    }

    private List<WorkflowDto.EdgeResponse> loadEdges(UUID workflowId) {
        return edgeQueryRepo.findByWorkflowId(workflowId).stream()
                .map(e -> new WorkflowDto.EdgeResponse(
                        e.getId().toString(),
                        e.getSourceStepId().toString(),
                        e.getTargetStepId().toString(),
                        e.getSourceHandle(),
                        e.getTargetHandle()
                )).toList();
    }

    private WorkflowDto.WorkflowDetailResponse toDetail(Workflow_query w,
                                                         List<WorkflowDto.StepResponse> steps,
                                                         List<WorkflowDto.EdgeResponse> edges) {
        return new WorkflowDto.WorkflowDetailResponse(
                w.getId().toString(),
                w.getName(),
                w.getDescription(),
                w.isActive(),
                w.getStatus().name(),
                w.getVersion(),
                steps,
                edges,
                w.getCreatedAt(),
                w.getUpdatedAt(),
                w.getLastRunAt()
        );
    }
}
