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
import java.util.Objects;
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
@SuppressWarnings("unchecked")
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
    public void execute(com.crescendo.logbook.workflow_run.WorkflowRun run) {
        UUID workflowRunId = run.getId();
        UUID workflowId = run.getWorkflowId();
        UUID userId = run.getUserId();
        Map<String, Object> triggerData = run.getTriggerData();

        List<Steps_command> steps = stepsRepo.findActiveByWorkflowIdOrdered(workflowId);
        List<Steps_command> executableSteps = steps.stream()
            .filter(step -> step.getType() != StepType.TRIGGER)
            .toList();

        // Accumulate outputs from all previous steps for variable replacement.
        // stepOrder (1-based index) -> output data map
        Map<Integer, Map<String, Object>> allStepOutputs = new java.util.HashMap<>();

        // Seed trigger payload at the trigger step order
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

        // If resuming, load execution state
        if (run.getExecutionState() != null) {
            Map<String, Object> stateMap = (Map<String, Object>) (Object) run.getExecutionState();
            for (Map.Entry<String, Object> entry : stateMap.entrySet()) {
                Map<String, Object> valMap = (Map<String, Object>) entry.getValue();
                allStepOutputs.put(Integer.parseInt(entry.getKey()), valMap);
            }
        }

        if (executableSteps.isEmpty()) {
            logger.warn("[engine] Workflow {} has no executable action steps — completing run {} immediately",
                    workflowId, workflowRunId);
            persistExecutionState(userId, workflowRunId, allStepOutputs);
            workflowRunService.completeRun(userId, workflowRunId);
            return;
        }

        logger.info("[engine] Starting execution of {} action step(s) for run {}",
            executableSteps.size(), workflowRunId);

        // The input to the *next* step directly
        Map<String, Object> previousOutput = triggerData != null ? triggerData : Map.of();

        if (run.getResumeStepId() != null) {
            executeFlatResume(run, executableSteps, previousOutput, allStepOutputs);
            return;
        }

        SequenceExecutionResult sequenceResult = executeSequence(
                executableSteps, null, null, workflowRunId, userId, previousOutput, allStepOutputs);

        if (!sequenceResult.success) {
            workflowRunService.failRun(userId, workflowRunId,
                    "Step '" + sequenceResult.failedStep.getName() + "' failed");
            return;
        }

        if (sequenceResult.suspended) {
            if (sequenceResult.resumeStep == null && sequenceResult.resumeToken == null) {
                persistExecutionState(userId, workflowRunId, allStepOutputs);
                workflowRunService.completeRun(userId, workflowRunId);
                return;
            }

            workflowRunService.suspendRun(userId, workflowRunId, sequenceResult.resumeAt,
                    sequenceResult.resumeStep != null ? sequenceResult.resumeStep.getId() : null,
                    sequenceResult.resumeToken, stringifiedOutputs(allStepOutputs));
            logger.info("[engine] Run {} suspended until {} to resume at step {}",
                    workflowRunId, sequenceResult.resumeAt,
                    sequenceResult.resumeStep != null ? sequenceResult.resumeStep.getName() : "<external completion>");
            return;
        }

        persistExecutionState(userId, workflowRunId, allStepOutputs);
        workflowRunService.completeRun(userId, workflowRunId);
        logger.info("[engine] Run {} completed successfully", workflowRunId);
    }

    private record StepExecutionResult(boolean success, boolean suspended, java.time.Instant resumeAt,
                                       String resumeToken, Map<String, Object> outputData) {}

    private void executeFlatResume(com.crescendo.logbook.workflow_run.WorkflowRun run,
                                   List<Steps_command> executableSteps,
                                   Map<String, Object> previousOutput,
                                   Map<Integer, Map<String, Object>> allStepOutputs) {
        UUID workflowRunId = run.getId();
        UUID userId = run.getUserId();
        int startIndex = 0;
        for (int i = 0; i < executableSteps.size(); i++) {
            if (executableSteps.get(i).getId().equals(run.getResumeStepId())) {
                startIndex = i;
                break;
            }
        }
        if (startIndex > 0) {
            Steps_command prevStep = executableSteps.get(startIndex - 1);
            if (allStepOutputs.containsKey(prevStep.getOrder().intValue())) {
                previousOutput = allStepOutputs.get(prevStep.getOrder().intValue());
            }
        }

        for (int i = startIndex; i < executableSteps.size(); i++) {
            Steps_command step = executableSteps.get(i);
            StepExecutionResult result = executeStep(step, workflowRunId, userId, previousOutput, allStepOutputs);

            if (!result.success) {
                workflowRunService.failRun(userId, workflowRunId,
                        "Step '" + step.getName() + "' failed");
                return;
            }

            previousOutput = result.outputData;
            allStepOutputs.put(step.getOrder().intValue(), result.outputData);

            if (result.suspended) {
                if (i == executableSteps.size() - 1) {
                    if (result.resumeToken == null) {
                        break;
                    }

                    workflowRunService.suspendRun(userId, workflowRunId, result.resumeAt, null,
                            result.resumeToken, stringifiedOutputs(allStepOutputs));
                    logger.info("[engine] Run {} suspended until {} for external completion",
                            workflowRunId, result.resumeAt);
                    return;
                }

                Steps_command nextStep = executableSteps.get(i + 1);
                workflowRunService.suspendRun(userId, workflowRunId, result.resumeAt, nextStep.getId(),
                        result.resumeToken, stringifiedOutputs(allStepOutputs));
                logger.info("[engine] Run {} suspended until {} to resume at step {}",
                        workflowRunId, result.resumeAt, nextStep.getName());
                return;
            }
        }

        persistExecutionState(userId, workflowRunId, allStepOutputs);
        workflowRunService.completeRun(userId, workflowRunId);
        logger.info("[engine] Run {} completed successfully", workflowRunId);
    }

    private record SequenceExecutionResult(boolean success, boolean suspended, java.time.Instant resumeAt,
                                           String resumeToken,
                                           Steps_command resumeStep, Steps_command failedStep,
                                           Map<String, Object> outputData) {
        static SequenceExecutionResult success(Map<String, Object> outputData) {
            return new SequenceExecutionResult(true, false, null, null, null, null, outputData);
        }

        static SequenceExecutionResult failure(Steps_command step) {
            return new SequenceExecutionResult(false, false, null, null, null, step, Map.of());
        }

        static SequenceExecutionResult suspended(java.time.Instant resumeAt, String resumeToken, Steps_command resumeStep, Map<String, Object> outputData) {
            return new SequenceExecutionResult(true, true, resumeAt, resumeToken, resumeStep, null, outputData);
        }
    }

    private SequenceExecutionResult executeSequence(List<Steps_command> allSteps, UUID parentStepId, String branchKey,
                                                    UUID workflowRunId, UUID userId,
                                                    Map<String, Object> inputData,
                                                    Map<Integer, Map<String, Object>> allStepOutputs) {
        List<Steps_command> sequenceSteps = allSteps.stream()
                .filter(step -> Objects.equals(step.getParentStepId(), parentStepId))
                .filter(step -> Objects.equals(normalizeBranchKey(step.getBranchKey()), normalizeBranchKey(branchKey)))
                .toList();

        Map<String, Object> previousOutput = inputData != null ? inputData : Map.of();

        for (int i = 0; i < sequenceSteps.size(); i++) {
            Steps_command step = sequenceSteps.get(i);
            StepExecutionResult result = executeStep(step, workflowRunId, userId, previousOutput, allStepOutputs);

            if (!result.success) {
                return SequenceExecutionResult.failure(step);
            }

            previousOutput = result.outputData;
            allStepOutputs.put(step.getOrder().intValue(), result.outputData);

            if (result.suspended) {
                Steps_command nextStep = i < sequenceSteps.size() - 1 ? sequenceSteps.get(i + 1) : null;
                return SequenceExecutionResult.suspended(result.resumeAt, result.resumeToken, nextStep, previousOutput);
            }

            String selectedBranch = selectedBranch(result.outputData);
            if (selectedBranch != null) {
                SequenceExecutionResult branchResult = executeSequence(
                        allSteps, step.getId(), selectedBranch, workflowRunId, userId, previousOutput, allStepOutputs);

                if (!branchResult.success) {
                    return branchResult;
                }
                previousOutput = branchResult.outputData;

                if (branchResult.suspended) {
                    Steps_command resumeStep = branchResult.resumeStep != null
                            ? branchResult.resumeStep
                            : (i < sequenceSteps.size() - 1 ? sequenceSteps.get(i + 1) : null);
                    return SequenceExecutionResult.suspended(branchResult.resumeAt, branchResult.resumeToken, resumeStep, previousOutput);
                }
            }
        }

        return SequenceExecutionResult.success(previousOutput);
    }

    private String selectedBranch(Map<String, Object> outputData) {
        if (outputData == null) return null;
        for (String key : List.of("_branchKey", "branchKey", "selectedBranch", "route", "path")) {
            Object value = outputData.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value).trim();
            }
        }
        return null;
    }

    private String normalizeBranchKey(String branchKey) {
        return branchKey == null || branchKey.isBlank() ? null : branchKey.trim();
    }

    private void persistExecutionState(UUID userId, UUID workflowRunId,
                                       Map<Integer, Map<String, Object>> allStepOutputs) {
        workflowRunService.saveExecutionState(userId, workflowRunId, stringifiedOutputs(allStepOutputs));
    }

    private Map<String, Object> stringifiedOutputs(Map<Integer, Map<String, Object>> allStepOutputs) {
        Map<String, Object> stringifiedOutputs = new java.util.HashMap<>();
        for (Map.Entry<Integer, Map<String, Object>> entry : allStepOutputs.entrySet()) {
            stringifiedOutputs.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return stringifiedOutputs;
    }

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
            return new StepExecutionResult(false, false, null, null, Map.of());
        }

        // 2. Create StepRun (RUNNING)
        LogbookDto.StepRunResponse stepRun = stepRunService.startStepRun(
                userId, workflowRunId, step.getId(), inputData);
        UUID stepRunId = UUID.fromString(stepRun.id());

        // 3. Load connection credentials — user connection or platform key fallback
        Map<String, Object> credentials = loadCredentials(step.getConnectionId(), appKey, userId);

        // 4. Resolve variable templates in configuration (e.g. {{steps.1.fieldName}})
        Map<String, Object> rawConfig = step.getConfiguration() != null ? step.getConfiguration() : Map.of();
        Map<String, Object> resolvedConfig = resolveTemplates(rawConfig, allStepOutputs);

        // 5. Build context and execute
        ActionContext context = new ActionContext(
                appKey, actionKey,
                resolvedConfig,
                credentials,
                inputData,
                workflowRunId,
                userId,
                step.getId(),
                step.getOrder().intValue()
        );

        try {
            ActionResult result = handler.execute(context);

            if (result.success()) {
                stepRunService.completeStepRun(userId, stepRunId, result.outputData());
                logger.debug("[engine] Step '{}' ({}:{}) succeeded", step.getName(), appKey, actionKey);
                return new StepExecutionResult(true, false, null, null, result.outputData());
            } else {
                stepRunService.failStepRun(userId, stepRunId, result.error());
                logger.warn("[engine] Step '{}' ({}:{}) failed: {}", step.getName(), appKey, actionKey, result.error());
                return new StepExecutionResult(false, false, null, null, Map.of());
            }
        } catch (com.crescendo.execution.action.SuspendExecutionException e) {
            logger.info("[engine] Step '{}' ({}:{}) requested suspension until {}", step.getName(), appKey, actionKey, e.getResumeAt());
            Map<String, Object> suspendedOutput = e.getOutputData() != null ? e.getOutputData() : inputData;
            stepRunService.completeStepRun(userId, stepRunId, suspendedOutput);
            return new StepExecutionResult(true, true, e.getResumeAt(), e.getResumeToken(), suspendedOutput);
        } catch (Exception e) {
            logger.error("[engine] Uncaught exception in step '{}' ({}:{})",
                    step.getName(), appKey, actionKey, e);
            stepRunService.failStepRun(userId, stepRunId,
                    "Unexpected error: " + e.getMessage());
            return new StepExecutionResult(false, false, null, null, Map.of());
        }
    }

    /**
     * Loads credentials for a step.
     *
     * <p>When a connectionId is present, enforces that the connection belongs to
     * the workflow owner (userId). This prevents a step whose connectionId was
     * forged or leaked from accessing another user's credentials.
     *
     * <p>Falls back to a platform-level API key when no user connection is present.
     */
    private Map<String, Object> loadCredentials(UUID connectionId, String appKey, UUID userId) {
        // 1. Try user connection first — with ownership enforcement
        if (connectionId != null) {
            try {
                // findByIdAndUser_Id enforces ownership at the DB level
                Connections_command connection = connectionsRepo
                        .findByIdAndUser_Id(connectionId, userId)
                        .orElse(null);
                if (connection != null) {
                    return tokenRefreshService.getValidCredentials(connection);
                }
                // Connection not found or owned by another user — fail fast
                logger.error("[engine] Connection {} not found or not owned by user {}",
                        connectionId, userId);
                throw new IllegalStateException(
                        "Connection " + connectionId + " not found or not owned by this user");
            } catch (IllegalStateException e) {
                throw e;
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
        // appendReplacement copies literal text between matches into sb, then appends our replacement.
        // appendTail copies any text after the last match. Together they rebuild the full string.
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
