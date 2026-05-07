package com.crescendo.steps;

import com.crescendo.enums.ConditionOperator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request and response DTOs for step-condition operations and
 * enriched step views that include condition data.
 *
 * Condition operations:
 *   CreateConditionRequest   — add a filter condition to a TRIGGER step
 *   ConditionResponse        — read representation of a condition
 *
 * Enhanced step views:
 *   StepDetailResponse       — single step with its conditions attached
 */
public class StepsDto {

    // =====================================================================
    // CONDITION REQUESTS
    // =====================================================================

    public record CreateConditionRequest(
            @NotBlank @Size(max = 255) String field,
            @NotNull ConditionOperator operator,
            @Size(max = 1000) String value
    ) {}

    // =====================================================================
    // CONDITION RESPONSES
    // =====================================================================

    public record ConditionResponse(
            UUID id,
            String field,
            String operator,
            String value,
            Instant createdAt
    ) {}

    // =====================================================================
    // STEP DETAIL (includes conditions)
    // =====================================================================

    /// Single step with its trigger conditions — used for step detail views.
    public record StepDetailResponse(
            String id,
            String name,
            String type,
            BigDecimal order,
            String appKey,
            String actionKey,
            UUID connectionId,
            Map<String, Object> configuration,
            List<ConditionResponse> conditions,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
