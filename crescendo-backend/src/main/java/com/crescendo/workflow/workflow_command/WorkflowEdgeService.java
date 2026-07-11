package com.crescendo.workflow.workflow_command;

import com.crescendo.shared.domain.event.DomainEventPublisher;
import com.crescendo.workflow.domain_event.edges.WorkflowEdgeCreatedEvent;
import com.crescendo.workflow.domain_event.edges.WorkflowEdgeDeletedEvent;
import com.crescendo.workflow.workflow_query.WorkflowEdge_query;
import com.crescendo.workflow.workflow_query.WorkflowEdge_queryRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Write-side service for workflow edge management.
 *
 * Responsibilities:
 *   1. Create/delete edges with CQRS sync (command + query tables).
 *   2. Publish WorkflowEdgeCreatedEvent / WorkflowEdgeDeletedEvent.
 *   3. Enforce DAG integrity: reject duplicate edges and cycles.
 *   4. Cascade-delete edges when a step or workflow is deleted.
 */
@Service
@Transactional(transactionManager = "commandTransactionManager")
public class WorkflowEdgeService {

    private final WorkflowEdge_commandRepository edgeCommandRepo;
    private final WorkflowEdge_queryRepository edgeQueryRepo;
    private final DomainEventPublisher eventPublisher;

    public WorkflowEdgeService(WorkflowEdge_commandRepository edgeCommandRepo,
                               WorkflowEdge_queryRepository edgeQueryRepo,
                               DomainEventPublisher eventPublisher) {
        this.edgeCommandRepo = edgeCommandRepo;
        this.edgeQueryRepo = edgeQueryRepo;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates a new edge. Validates:
     *   - Not a self-loop
     *   - No duplicate source→target pair
     *   - No cycle would be introduced (DAG enforcement)
     */
    public WorkflowEdge_command createEdge(UUID workflowId,
                                           UUID sourceStepId,
                                           UUID targetStepId,
                                           String sourceHandle,
                                           String targetHandle) {
        if (sourceStepId.equals(targetStepId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A step cannot connect to itself.");
        }

        // Duplicate edge check
        List<WorkflowEdge_command> existing = edgeCommandRepo.findByWorkflowId(workflowId);
        boolean duplicate = existing.stream()
                .anyMatch(e -> e.getSourceStepId().equals(sourceStepId) && e.getTargetStepId().equals(targetStepId));
        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This connection already exists.");
        }

        // Cycle check — DFS from target; if we reach source, adding this edge creates a cycle
        if (wouldCreateCycle(sourceStepId, targetStepId, existing)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Connection would create a cycle.");
        }

        UUID edgeId = UUID.randomUUID();
        WorkflowEdge_command edge = new WorkflowEdge_command(
                edgeId, workflowId, sourceStepId, targetStepId,
                normalizeHandle(sourceHandle), normalizeHandle(targetHandle));
        edgeCommandRepo.save(edge);

        // CQRS sync
        WorkflowEdge_query queryEdge = new WorkflowEdge_query(
                edgeId, workflowId, sourceStepId, targetStepId,
                normalizeHandle(sourceHandle), normalizeHandle(targetHandle));
        edgeQueryRepo.save(queryEdge);

        eventPublisher.publish(new WorkflowEdgeCreatedEvent(edgeId, workflowId, sourceStepId, targetStepId,
                normalizeHandle(sourceHandle), normalizeHandle(targetHandle)));

        return edge;
    }

    /**
     * Deletes a single edge by ID.
     */
    public void deleteEdge(UUID edgeId, UUID workflowId) {
        WorkflowEdge_command edge = edgeCommandRepo.findById(edgeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Edge not found."));
        if (!edge.getWorkflowId().equals(workflowId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Edge not found in this workflow.");
        }
        edgeCommandRepo.delete(edge);
        edgeQueryRepo.deleteById(edgeId);
        eventPublisher.publish(new WorkflowEdgeDeletedEvent(edgeId, workflowId));
    }

    /**
     * Replaces ALL edges for a workflow atomically.
     * Used by the graph-save endpoint — delete everything and re-insert.
     */
    public void replaceAllEdges(UUID workflowId, List<EdgeSpec> specs) {
        // Delete existing
        edgeCommandRepo.deleteByWorkflowId(workflowId);
        edgeQueryRepo.deleteByWorkflowId(workflowId);

        // Validate + insert new edges
        // We build a running adjacency list to validate cycles incrementally
        List<WorkflowEdge_command> created = new ArrayList<>();
        for (EdgeSpec spec : specs) {
            if (spec.sourceStepId().equals(spec.targetStepId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A step cannot connect to itself.");
            }
            boolean dup = created.stream()
                    .anyMatch(e -> e.getSourceStepId().equals(spec.sourceStepId()) && e.getTargetStepId().equals(spec.targetStepId()));
            if (dup) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Duplicate edge from " + spec.sourceStepId() + " to " + spec.targetStepId());
            }
            if (wouldCreateCycle(spec.sourceStepId(), spec.targetStepId(), created)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Connection from " + spec.sourceStepId() + " to " + spec.targetStepId() + " would create a cycle.");
            }
            UUID edgeId = UUID.randomUUID();
            WorkflowEdge_command edge = new WorkflowEdge_command(
                    edgeId, workflowId, spec.sourceStepId(), spec.targetStepId(),
                    normalizeHandle(spec.sourceHandle()), normalizeHandle(spec.targetHandle()));
            edgeCommandRepo.save(edge);
            edgeQueryRepo.save(new WorkflowEdge_query(
                    edgeId, workflowId, spec.sourceStepId(), spec.targetStepId(),
                    normalizeHandle(spec.sourceHandle()), normalizeHandle(spec.targetHandle())));
            eventPublisher.publish(new WorkflowEdgeCreatedEvent(edgeId, workflowId,
                    spec.sourceStepId(), spec.targetStepId(),
                    normalizeHandle(spec.sourceHandle()), normalizeHandle(spec.targetHandle())));
            created.add(edge);
        }
    }

    /**
     * Deletes all edges for a workflow (used during workflow deletion).
     */
    public void deleteAllEdgesForWorkflow(UUID workflowId) {
        edgeCommandRepo.deleteByWorkflowId(workflowId);
        edgeQueryRepo.deleteByWorkflowId(workflowId);
    }

    /**
     * Deletes all edges connected to a step (used during step deletion).
     */
    public void deleteEdgesForStep(UUID stepId) {
        edgeCommandRepo.deleteBySourceStepIdOrTargetStepId(stepId);
        edgeQueryRepo.deleteBySourceStepIdOrTargetStepId(stepId);
    }

    /**
     * Immutable spec for an edge to be created.
     */
    public record EdgeSpec(UUID sourceStepId, UUID targetStepId, String sourceHandle, String targetHandle) {}

    // ── Cycle Detection ────────────────────────────────────────────────────────

    /**
     * Returns true if adding an edge source→target would create a cycle,
     * by checking whether target can already reach source via DFS.
     */
    private boolean wouldCreateCycle(UUID source, UUID target, List<WorkflowEdge_command> existing) {
        // Build adjacency from existing edges
        Map<UUID, List<UUID>> adj = new HashMap<>();
        for (WorkflowEdge_command e : existing) {
            adj.computeIfAbsent(e.getSourceStepId(), k -> new ArrayList<>()).add(e.getTargetStepId());
        }
        // DFS from target — if we reach source, a cycle would form
        Set<UUID> visited = new HashSet<>();
        return dfsReaches(adj, target, source, visited);
    }

    private boolean dfsReaches(Map<UUID, List<UUID>> adj, UUID current, UUID goal, Set<UUID> visited) {
        if (current.equals(goal)) return true;
        if (!visited.add(current)) return false;
        for (UUID next : adj.getOrDefault(current, List.of())) {
            if (dfsReaches(adj, next, goal, visited)) return true;
        }
        return false;
    }

    private String normalizeHandle(String handle) {
        return handle == null || handle.isBlank() ? null : handle.trim();
    }
}
