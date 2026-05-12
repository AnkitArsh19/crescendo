package com.crescendo.execution.engine;

import com.crescendo.admin.PlatformKey;
import com.crescendo.admin.PlatformKeyRepository;
import com.crescendo.connections.connections_command.Connections_command;
import com.crescendo.connections.connections_command.Connections_commandRepository;
import com.crescendo.connections.oauth.OAuthTokenRefreshService;
import com.crescendo.connections.security.ConnectionCredentialsCryptoService;
import com.crescendo.enums.StepType;
import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionHandlerRegistry;
import com.crescendo.execution.action.ActionResult;
import com.crescendo.logbook.LogbookDto;
import com.crescendo.logbook.step_run.StepRunService;
import com.crescendo.logbook.workflow_run.WorkflowRunService;
import com.crescendo.steps.steps_command.Steps_command;
import com.crescendo.steps.steps_command.Steps_commandRepository;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orchestrates step-by-step execution of a workflow run.
 * <p>
 * Called by {@code ExecutionQueueConsumer} after the run has been transitioned
 * to RUNNING and a distributed lock has been acquired.
 * <p>
 * Execution flow:
 * <ol>
 *   <li>Load steps ordered by {@code step_order} ASC</li>
 *   <li>For each step: create a StepRun, resolve the ActionHandler, execute, record result</li>
 *   <li>Output of step N becomes input of step N+1 (data chaining)</li>
 *   <li>On first failure: abort remaining steps and fail the workflow run</li>
 *   <li>All steps succeed: complete the workflow run</li>
 * </ol>
 */
@Service
public class WorkflowExecutionEngine {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutionEngine.class);

    private final Steps_commandRepository stepsRepo;
    private final Connections_commandRepository connectionsRepo;
    private final ActionHandlerRegistry handlerRegistry;
    private final ConnectionCredentialsCryptoService credentialsCryptoService;
    private final OAuthTokenRefreshService tokenRefreshService;
    private final StepRunService stepRunService;
    private final WorkflowRunService workflowRunService;
    private final PlatformKeyRepository platformKeyRepo;
    private final ObjectMapper objectMapper;

    public WorkflowExecutionEngine(Steps_commandRepository stepsRepo,
                                    Connections_commandRepository connectionsRepo,
                                    ActionHandlerRegistry handlerRegistry,
                                    ConnectionCredentialsCryptoService credentialsCryptoService,
                                    OAuthTokenRefreshService tokenRefreshService,
                                    StepRunService stepRunService,
                                    WorkflowRunService workflowRunService,
                                    PlatformKeyRepository platformKeyRepo,
                                    ObjectMapper objectMapper) {
        this.stepsRepo = stepsRepo;
        this.connectionsRepo = connectionsRepo;
        this.handlerRegistry = handlerRegistry;
        this.credentialsCryptoService = credentialsCryptoService;
        this.tokenRefreshService = tokenRefreshService;
        this.stepRunService = stepRunService;
        this.workflowRunService = workflowRunService;
        this.platformKeyRepo = platformKeyRepo;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes all steps for a workflow run sequentially.
     *
     * @param workflowRunId the run being executed
     * @param workflowId    the parent workflow (for step lookup)
     * @param userId        owner of the workflow (for service-level ownership checks)
     * @param triggerData   the initial trigger payload (webhook body, manual input, etc.)
     */
    public void execute(UUID workflowRunId, UUID workflowId, UUID userId,
                        Map<String, Object> triggerData) {

        List<Steps_command> steps = stepsRepo.findActiveByWorkflowIdOrdered(workflowId);
        List<Steps_command> executableSteps = steps.stream()
            .filter(step -> step.getType() != StepType.TRIGGER)
            .toList();

        if (executableSteps.isEmpty()) {
            logger.warn("[engine] Workflow {} has no executable action steps — completing run {} immediately",
                    workflowId, workflowRunId);
            workflowRunService.completeRun(userId, workflowRunId);
            return;
        }

        logger.info("[engine] Starting execution of {} action step(s) for run {}",
            executableSteps.size(), workflowRunId);

        // Accumulate outputs from all previous steps for variable replacement.
        // stepOrder (1-based index) -> output data map
        Map<Integer, Map<String, Object>> allStepOutputs = new java.util.HashMap<>();
        
        // Seed trigger payload at the trigger step order so templates like
        // {{steps.1.field}} can resolve against the trigger output.
        if (triggerData != null) {
            Steps_command triggerStep = steps.stream()
                    .filter(step -> step.getType() == StepType.TRIGGER)
                    .findFirst()
                    .orElse(null);
            if (triggerStep != null && triggerStep.getOrder() != null) {
                allStepOutputs.put(triggerStep.getOrder().intValue(), triggerData);
            } else {
                allStepOutputs.put(0, triggerData);
            }
        }

        // The input to the *next* step directly (legacy, but we still pass it for ActionContext)
        Map<String, Object> previousOutput = triggerData != null ? triggerData : Map.of();

        for (Steps_command step : executableSteps) {
            StepExecutionResult result = executeStep(step, workflowRunId, userId, previousOutput, allStepOutputs);
            if (!result.success) {
                workflowRunService.failRun(userId, workflowRunId,
                        "Step '" + step.getName() + "' failed");
                return;
            }
            previousOutput = result.outputData;
            allStepOutputs.put(step.getOrder().intValue(), result.outputData);
        }

        workflowRunService.completeRun(userId, workflowRunId);
        logger.info("[engine] Run {} completed successfully", workflowRunId);
    }

    private record StepExecutionResult(boolean success, Map<String, Object> outputData) {}

    /**
     * Executes a single step: creates a StepRun, resolves the handler, invokes it,
     * and records the result.
     */
    private StepExecutionResult executeStep(Steps_command step, UUID workflowRunId,
                                            UUID userId, Map<String, Object> inputData,
                                            Map<Integer, Map<String, Object>> allStepOutputs) {

        String appKey = step.getAppKey();
        String actionKey = step.getActionKey();

        // 1. Resolve handler
        ActionHandler handler = handlerRegistry.find(appKey, actionKey).orElse(null);
        if (handler == null) {
            logger.error("[engine] No handler registered for {}:{}", appKey, actionKey);
            LogbookDto.StepRunResponse stepRun = stepRunService.startStepRun(
                    userId, workflowRunId, step.getId(), inputData);
            stepRunService.failStepRun(userId, UUID.fromString(stepRun.id()),
                    "No action handler registered for " + appKey + ":" + actionKey);
            return new StepExecutionResult(false, Map.of());
        }

        // 2. Create StepRun (RUNNING)
        LogbookDto.StepRunResponse stepRun = stepRunService.startStepRun(
                userId, workflowRunId, step.getId(), inputData);
        UUID stepRunId = UUID.fromString(stepRun.id());

        // 3. Load connection credentials — user connection or platform key fallback
        Map<String, Object> credentials = loadCredentials(step.getConnectionId(), appKey);

        // 4. Resolve variable templates in configuration (e.g. {{steps.1.fieldName}})
        Map<String, Object> rawConfig = step.getConfiguration() != null ? step.getConfiguration() : Map.of();
        Map<String, Object> resolvedConfig = resolveTemplates(rawConfig, allStepOutputs);

        // 5. Build context and execute
        ActionContext context = new ActionContext(
                appKey, actionKey,
                resolvedConfig,
                credentials,
                inputData
        );

        try {
            ActionResult result = handler.execute(context);

            if (result.success()) {
                stepRunService.completeStepRun(userId, stepRunId, result.outputData());
                logger.debug("[engine] Step '{}' ({}:{}) succeeded", step.getName(), appKey, actionKey);
                return new StepExecutionResult(true, result.outputData());
            } else {
                stepRunService.failStepRun(userId, stepRunId, result.error());
                logger.warn("[engine] Step '{}' ({}:{}) failed: {}", step.getName(), appKey, actionKey, result.error());
                return new StepExecutionResult(false, Map.of());
            }
        } catch (Exception e) {
            logger.error("[engine] Uncaught exception in step '{}' ({}:{})",
                    step.getName(), appKey, actionKey, e);
            stepRunService.failStepRun(userId, stepRunId,
                    "Unexpected error: " + e.getMessage());
            return new StepExecutionResult(false, Map.of());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadCredentials(UUID connectionId, String appKey) {
        // 1. Try user connection first
        if (connectionId != null) {
            try {
                Connections_command connection = connectionsRepo.findById(connectionId).orElse(null);
                if (connection != null) {
                    return tokenRefreshService.getValidCredentials(connection);
                }
                logger.warn("[engine] Connection {} not found", connectionId);
            } catch (Exception e) {
                logger.error("[engine] Failed to load credentials for connection {}: {}",
                        connectionId, e.getMessage());
                throw e;
            }
        }

        // 2. Fall back to platform key if no user connection
        try {
            PlatformKey pk = platformKeyRepo.findByAppKeyAndEnabledTrue(appKey).orElse(null);
            if (pk != null && pk.getEncryptedCredentials() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> sealed = objectMapper
                        .readValue(pk.getEncryptedCredentials(), Map.class);
                Map<String, Object> opened = credentialsCryptoService.open(sealed);
                pk.incrementUsageCount();
                platformKeyRepo.save(pk);
                logger.debug("[engine] Using platform key for app '{}'", appKey);
                return opened;
            }
        } catch (Exception e) {
            logger.warn("[engine] Failed to load platform key for app '{}': {}", appKey, e.getMessage());
        }

        return Map.of();
    }

    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{steps\\.(\\d+)\\.([^}]+)}}");

    /**
     * Deeply resolves templates in a configuration map.
     * Currently supports string values containing {{steps.N.fieldName}}.
     */
    private Map<String, Object> resolveTemplates(Map<String, Object> config, Map<Integer, Map<String, Object>> allStepOutputs) {
        if (config == null || config.isEmpty()) return config;
        
        Map<String, Object> resolved = new java.util.HashMap<>();
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String strVal) {
                resolved.put(entry.getKey(), resolveStringTemplate(strVal, allStepOutputs));
            } else {
                // For now, only top-level string replacements are supported.
                // Could be extended to nested maps/lists if necessary.
                resolved.put(entry.getKey(), value);
            }
        }
        return resolved;
    }

    private String resolveStringTemplate(String template, Map<Integer, Map<String, Object>> allStepOutputs) {
        if (template == null || !template.contains("{{")) return template;

        Matcher matcher = TEMPLATE_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            int stepIndex = Integer.parseInt(matcher.group(1));
            String fieldName = matcher.group(2).trim();

            String replacement = "";
            Map<String, Object> stepOut = allStepOutputs.get(stepIndex);
            if (stepOut != null) {
                Object val = stepOut.get(fieldName);
                if (val != null) {
                    replacement = String.valueOf(val);
                } else if (fieldName.contains(".")) { // basic nested property support e.g. "data.id"
                    replacement = String.valueOf(resolveNestedProperty(stepOut, fieldName.split("\\.")));
                }
            }
            logger.debug("[engine] Resolved template {{steps.{}.{}}} -> {}", stepIndex, fieldName, replacement);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private Object resolveNestedProperty(Map<String, Object> map, String[] parts) {
        Object current = map;
        for (String part : parts) {
            if (current instanceof Map<?,?> m) {
                current = m.get(part);
            } else {
                return null;
            }
        }
        return current;
    }
}
