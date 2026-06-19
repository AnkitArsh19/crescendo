package com.crescendo.workflow;

import com.crescendo.enums.StepType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * All request and response DTOs for workflow and step operations.
 *
 * Workflow operations:
 *   CreateWorkflowRequest     — create a new workflow (name required, description optional)
 *   UpdateWorkflowRequest     — update workflow name and/or description
 *   WorkflowSummaryResponse   — list-view representation (no steps)
 *   WorkflowDetailResponse    — detail-view representation (includes steps)
 *
 * Step operations:
 *   CreateStepRequest          — add a step to a workflow
 *   UpdateStepRequest          — modify an existing step
 *   ReorderStepRequest         — change a step's position in the workflow
 *   StepResponse               — read representation of a step
 */
public class WorkflowDto {

    //WORKFLOW REQUESTS

    public record CreateWorkflowRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 500) String description
    ) {}

    public record UpdateWorkflowRequest(
            @Size(min = 1, max = 255) String name,
            @Size(max = 500) String description
    ) {}

    //STEP REQUESTS

    public record CreateStepRequest(
            @NotBlank @Size(max = 255) String name,
            @NotNull StepType type,
            @NotBlank String actionKey,
            @NotBlank String appKey,
            UUID connectionId,
            UUID parentStepId,
            String branchKey,
            Map<String, Object> configuration
    ) {}

    public record UpdateStepRequest(
            @Size(min = 1, max = 255) String name,
            String actionKey,
            String appKey,
            UUID connectionId,
            UUID parentStepId,
            String branchKey,
            Map<String, Object> configuration
    ) {}

    public record ReorderStepRequest(
            @NotNull BigDecimal newOrder
    ) {}

    // WORKFLOW RESPONSES

    /// Summary view used in list endpoints — no step detail.
    public record WorkflowSummaryResponse(
            String id,
            String name,
            String description,
            boolean isActive,
            String status,
            int stepCount,
            Instant createdAt,
            Instant updatedAt,
            Instant lastRunAt
    ) {}

    /// Detail view used in single-workflow GET — includes ordered steps.
    public record WorkflowDetailResponse(
            String id,
            String name,
            String description,
            boolean isActive,
            String status,
            List<StepResponse> steps,
            Instant createdAt,
            Instant updatedAt,
            Instant lastRunAt
    ) {}

    ///  STEP RESPONSES
    public record StepResponse(
            String id,
            String name,
            String type,
            BigDecimal order,
            String appKey,
            String actionKey,
            UUID connectionId,
            UUID parentStepId,
            String branchKey,
            Map<String, Object> configuration,
            Instant createdAt,
            Instant updatedAt
    ) {}

    // SHARED WORKFLOW RESPONSES (public — no connectionId)

    /// Shared workflow preview — includes full config but strips connectionId.
    /// Recipients see what the workflow does but must connect their own accounts.
    public record SharedWorkflowResponse(
            String id,
            String name,
            String description,
            List<SharedStepResponse> steps
    ) {}

    /// Step in a shared workflow — everything except connectionId.
    public record SharedStepResponse(
            String sourceStepId,
            String name,
            String type,
            BigDecimal order,
            String appKey,
            String actionKey,
            UUID parentStepId,
            String branchKey,
            Map<String, Object> configuration
    ) {}

    // BULK OPERATIONS

    public record BulkWorkflowRequest(
            @NotNull List<String> ids
    ) {}

    // IMPORT

    /// Import a shared workflow — creates a copy under the current user.
    public record ImportWorkflowRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 500) String description,
            List<ImportStepRequest> steps
    ) {}

    public record ImportStepRequest(
            String sourceStepId,
            @NotBlank @Size(max = 255) String name,
            @NotNull String type,
            @NotBlank String actionKey,
            @NotBlank String appKey,
            BigDecimal order,
            UUID parentStepId,
            String branchKey,
            Map<String, Object> configuration
    ) {}

    // GRAPH SAVE

    public record WorkflowGraphRequest(
            @Size(max = 255) String name,
            @Size(max = 500) String description,
            String revision,
            List<GraphStepRequest> steps,
            List<String> deletedStepIds
    ) {}

    public record GraphStepRequest(
            @NotBlank String clientId,
            String backendId,
            @NotNull StepType type,
            @NotBlank String name,
            @NotBlank String actionKey,
            @NotBlank String appKey,
            UUID connectionId,
            UUID parentStepId,
            String branchKey,
            Map<String, Object> configuration
    ) {}

    public record WorkflowGraphResponse(
            String id,
            String revision,
            List<GraphStepResponse> savedSteps
    ) {}

    public record GraphStepResponse(
            String clientId,
            String backendId
    ) {}
}
