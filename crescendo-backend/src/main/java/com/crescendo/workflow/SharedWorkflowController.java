package com.crescendo.workflow;

import com.crescendo.steps.steps_query.Steps_queryService;
import com.crescendo.workflow.workflow_query.Workflow_query;
import com.crescendo.workflow.workflow_query.Workflow_queryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Public (unauthenticated) endpoint for previewing shared workflows.
 *
 * When a user shares workflows via link, the link encodes workflow IDs.
 * This endpoint returns workflow details with steps (name, type, appKey,
 * actionKey, config, order) but WITHOUT connectionId — recipients must
 * connect their own accounts after importing.
 *
 * Rate-limiting and max ID count (20) prevent enumeration attacks.
 */
@RestController
@RequestMapping("/shared")
public class SharedWorkflowController {

    private static final int MAX_SHARED_IDS = 20;

    private final Workflow_queryRepository workflowQueryRepo;
    private final Steps_queryService stepsQueryService;

    public SharedWorkflowController(Workflow_queryRepository workflowQueryRepo,
                                     Steps_queryService stepsQueryService) {
        this.workflowQueryRepo = workflowQueryRepo;
        this.stepsQueryService = stepsQueryService;
    }

    /**
     * GET /shared/workflows?ids=uuid1,uuid2,...
     * Returns shared workflow previews with sanitized step data (no connectionId).
     */
    @GetMapping("/workflows")
    public ResponseEntity<List<WorkflowDto.SharedWorkflowResponse>> getSharedWorkflows(
            @RequestParam List<UUID> ids) {

        if (ids == null || ids.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one workflow ID is required");
        }
        if (ids.size() > MAX_SHARED_IDS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum " + MAX_SHARED_IDS + " workflows can be shared at once");
        }

        List<Workflow_query> workflows = workflowQueryRepo.findAllByIdIn(ids);

        List<WorkflowDto.SharedWorkflowResponse> responses = workflows.stream()
                .map(this::toSharedResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    /// Maps a query-side workflow + its steps to the shared response DTO.
    /// Steps include full config but strip connectionId.
    private WorkflowDto.SharedWorkflowResponse toSharedResponse(Workflow_query workflow) {
        List<WorkflowDto.StepResponse> rawSteps = stepsQueryService.loadSteps(workflow.getId());

        List<WorkflowDto.SharedStepResponse> sharedSteps = rawSteps.stream()
                .map(s -> new WorkflowDto.SharedStepResponse(
                        s.name(),
                        s.type(),
                        s.order(),
                        s.appKey(),
                        s.actionKey(),
                        s.configuration()
                ))
                .toList();

        return new WorkflowDto.SharedWorkflowResponse(
                workflow.getId().toString(),
                workflow.getName(),
                workflow.getDescription(),
                sharedSteps
        );
    }
}
