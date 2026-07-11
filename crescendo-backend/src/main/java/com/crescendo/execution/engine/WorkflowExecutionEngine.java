package com.crescendo.execution.engine;

import com.crescendo.admin.PlatformKey;
import com.crescendo.admin.PlatformKeyRepository;
import com.crescendo.connections.connections_command.Connections_command;
import com.crescendo.connections.connections_command.Connections_commandRepository;
import com.crescendo.connections.oauth.OAuthTokenRefreshService;
import com.crescendo.connections.security.ConnectionCredentialsCryptoService;
import com.crescendo.enums.CredentialSource;
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
import com.crescendo.user.user_query.User_query;
import com.crescendo.user.user_query.User_queryRepository;
import com.crescendo.enums.UserRole;
import com.crescendo.workflow.workflow_command.WorkflowEdge_command;
import com.crescendo.workflow.workflow_command.WorkflowEdge_commandRepository;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;
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
    private final WorkflowEdge_commandRepository edgeRepo;
    private final Connections_commandRepository connectionsRepo;
    private final ActionHandlerRegistry handlerRegistry;
    private final ConnectionCredentialsCryptoService credentialsCryptoService;
    private final OAuthTokenRefreshService tokenRefreshService;
    private final StepRunService stepRunService;
    private final WorkflowRunService workflowRunService;
    private final PlatformKeyRepository platformKeyRepo;
    private final User_queryRepository userQueryRepo;
    private final ObjectMapper objectMapper;

    public WorkflowExecutionEngine(Steps_commandRepository stepsRepo,
                                    WorkflowEdge_commandRepository edgeRepo,
                                    Connections_commandRepository connectionsRepo,
                                    ActionHandlerRegistry handlerRegistry,
                                    ConnectionCredentialsCryptoService credentialsCryptoService,
                                    OAuthTokenRefreshService tokenRefreshService,
                                    StepRunService stepRunService,
                                    WorkflowRunService workflowRunService,
                                    PlatformKeyRepository platformKeyRepo,
                                    User_queryRepository userQueryRepo,
                                    ObjectMapper objectMapper) {
        this.stepsRepo = stepsRepo;
        this.edgeRepo = edgeRepo;
        this.connectionsRepo = connectionsRepo;
        this.handlerRegistry = handlerRegistry;
        this.credentialsCryptoService = credentialsCryptoService;
        this.tokenRefreshService = tokenRefreshService;
        this.stepRunService = stepRunService;
        this.workflowRunService = workflowRunService;
        this.platformKeyRepo = platformKeyRepo;
        this.userQueryRepo = userQueryRepo;
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

        // Load all active steps as a map for O(1) lookup
        List<Steps_command> allSteps = stepsRepo.findActiveByWorkflowIdOrdered(workflowId);
        Map<UUID, Steps_command> stepById = new java.util.LinkedHashMap<>();
        for (Steps_command s : allSteps) stepById.put(s.getId(), s);

        // Load edges
        List<WorkflowEdge_command> edges = edgeRepo.findByWorkflowId(workflowId);

        // Build adjacency maps
        Map<UUID, List<UUID>> outgoing = new java.util.HashMap<>();
        Map<UUID, List<UUID>> incoming = new java.util.HashMap<>();
        for (UUID id : stepById.keySet()) {
            outgoing.put(id, new java.util.ArrayList<>());
            incoming.put(id, new java.util.ArrayList<>());
        }
        for (WorkflowEdge_command e : edges) {
            if (stepById.containsKey(e.getSourceStepId()) && stepById.containsKey(e.getTargetStepId())) {
                outgoing.get(e.getSourceStepId()).add(e.getTargetStepId());
                incoming.get(e.getTargetStepId()).add(e.getSourceStepId());
            }
        }

        // Find trigger step
        Steps_command triggerStep = allSteps.stream()
                .filter(s -> s.getType() == StepType.TRIGGER)
                .findFirst().orElse(null);

        // allStepOutputs keyed by stepId (UUID)
        Map<UUID, Map<String, Object>> allStepOutputs = new java.util.HashMap<>();

        // Seed trigger data
        if (triggerData != null && triggerStep != null) {
            allStepOutputs.put(triggerStep.getId(), triggerData);
        }

        // If resuming, restore saved state
        if (run.getExecutionState() != null) {
            Map<String, Object> stateMap = (Map<String, Object>) (Object) run.getExecutionState();
            for (Map.Entry<String, Object> entry : stateMap.entrySet()) {
                try {
                    UUID stepId = UUID.fromString(entry.getKey());
                    allStepOutputs.put(stepId, (Map<String, Object>) entry.getValue());
                } catch (IllegalArgumentException ignored) { /* legacy integer keys from old format */ }
            }
        }

        // Detect reachable steps from trigger via DFS (orphan detection)
        Set<UUID> reachable = new java.util.HashSet<>();
        if (triggerStep != null) {
            dfsReachable(triggerStep.getId(), outgoing, reachable);
        }

        // Determine which action steps to run (non-trigger, reachable)
        Set<UUID> skipped = new java.util.HashSet<>();
        for (UUID id : stepById.keySet()) {
            Steps_command s = stepById.get(id);
            if (s.getType() == StepType.TRIGGER) continue;
            if (!reachable.contains(id)) {
                skipped.add(id);
                logger.warn("[engine] Step '{}' ({}) is orphaned (not reachable from trigger) — will be skipped",
                        s.getName(), id);
            }
        }

        // Topological sort (Kahn's algorithm) over all reachable non-trigger steps
        List<UUID> topoOrder = topoSort(stepById.keySet(), incoming, outgoing, skipped,
                triggerStep != null ? triggerStep.getId() : null);

        if (topoOrder.isEmpty() && !skipped.isEmpty()) {
            // Everything is skipped or no action steps
        }

        if (topoOrder.isEmpty()) {
            logger.warn("[engine] Workflow {} has no executable action steps — completing run {} immediately",
                    workflowId, workflowRunId);
            persistExecutionState(userId, workflowRunId, allStepOutputs);
            workflowRunService.completeRun(userId, workflowRunId);
            return;
        }

        logger.info("[engine] Starting DAG execution of {} action step(s) for run {}",
                topoOrder.size(), workflowRunId);

        // Node state machine (0=PENDING, 1=COMPLETED, 2=SKIPPED, 3=FAILED)
        // Using int constants instead of local enum to avoid EnumMap type inference issues
        final int PENDING = 0, COMPLETED = 1, SKIPPED = 2, FAILED = 3;
        Map<UUID, Integer> nodeState = new java.util.HashMap<>();
        // Initialize: orphaned = SKIPPED, trigger = COMPLETED, others = PENDING
        for (UUID id : stepById.keySet()) {
            nodeState.put(id, skipped.contains(id) ? SKIPPED : PENDING);
        }
        if (triggerStep != null) nodeState.put(triggerStep.getId(), COMPLETED);

        // Execute in topological order
        for (UUID stepId : topoOrder) {
            Steps_command step = stepById.get(stepId);
            if (step == null) continue;

            List<UUID> parents = incoming.getOrDefault(stepId, List.of());

            // Skip-aware join check
            if (!parents.isEmpty()) {
                boolean anyFailed = parents.stream().anyMatch(p -> nodeState.get(p) == FAILED);
                if (anyFailed) {
                    nodeState.put(stepId, FAILED);
                    logger.warn("[engine] Step '{}' ({}) propagated FAILED from upstream", step.getName(), stepId);
                    continue;
                }

                boolean allParentsSkipped = parents.stream().allMatch(p -> nodeState.get(p) == SKIPPED);
                if (allParentsSkipped) {
                    nodeState.put(stepId, SKIPPED);
                    logger.info("[engine] Step '{}' ({}) SKIPPED (all incoming branches were skipped)", step.getName(), stepId);
                    continue;
                }

                // Logic/merge action: skip-aware AND join — wait for all non-skipped branches
                boolean isMergeStep = "logic".equals(step.getAppKey()) && "merge".equals(step.getActionKey());
                if (isMergeStep) {
                    List<UUID> takenParents = parents.stream()
                            .filter(p -> nodeState.get(p) != SKIPPED)
                            .toList();
                    boolean allTakenCompleted = takenParents.stream().allMatch(p -> nodeState.get(p) == COMPLETED);
                    if (!allTakenCompleted) {
                        nodeState.put(stepId, SKIPPED);
                        continue;
                    }
                    // Merge data from all completed parents
                    Map<String, Object> mergedInput = new java.util.HashMap<>();
                    for (UUID p : takenParents) {
                        Map<String, Object> pOut = allStepOutputs.getOrDefault(p, Map.of());
                        mergedInput.putAll(pOut);
                    }
                    allStepOutputs.put(stepId, mergedInput); // seed merged input before execution
                }
            }

            // Gather input from the first completed parent (skip-aware OR join default)
            Map<String, Object> inputData = Map.of();
            if (!parents.isEmpty()) {
                inputData = parents.stream()
                        .filter(p -> nodeState.get(p) == COMPLETED)
                        .map(allStepOutputs::get)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(Map.of());
            } else if (triggerStep != null) {
                inputData = allStepOutputs.getOrDefault(triggerStep.getId(), Map.of());
            }

            StepExecutionResult result = executeStep(step, workflowRunId, userId, inputData, allStepOutputs);

            if (!result.success) {
                nodeState.put(stepId, FAILED);
                logger.warn("[engine] Step '{}' failed — propagating failure downstream", step.getName());
                // Continue topo iteration so downstream nodes get FAILED state set
                continue;
            }

            allStepOutputs.put(stepId, result.outputData);
            nodeState.put(stepId, COMPLETED);

            // Handle if/else branch routing: mark the non-selected outgoing branches as SKIPPED
            String selectedBranchHandle = selectedBranch(result.outputData);
            if (selectedBranchHandle != null) {
                // Iterate all edges from this step; skip children on non-selected handles.
                for (WorkflowEdge_command edge : edges) {
                    if (edge.getSourceStepId().equals(stepId)) {
                        String handle = edge.getSourceHandle();
                        if (!selectedBranchHandle.equals(handle)) {
                            UUID childId = edge.getTargetStepId();
                            if (nodeState.get(childId) == PENDING) {
                                nodeState.put(childId, SKIPPED);
                                logger.info("[engine] Skipping branch '{}' from step '{}'", handle, step.getName());
                            }
                        }
                    }
                }
            }

            if (result.suspended) {
                // Suspend — save state and exit
                workflowRunService.suspendRun(userId, workflowRunId, result.resumeAt,
                        step.getId(), result.resumeToken, stringifiedOutputs(allStepOutputs));
                logger.info("[engine] Run {} suspended at step '{}'", workflowRunId, step.getName());
                return;
            }
        }

        // Check overall result: fail if any FAILED state
        boolean anyFailed = nodeState.values().stream().anyMatch(s -> s == FAILED);
        persistExecutionState(userId, workflowRunId, allStepOutputs);
        if (anyFailed) {
            workflowRunService.failRun(userId, workflowRunId, "One or more steps failed");
        } else {
            workflowRunService.completeRun(userId, workflowRunId);
            logger.info("[engine] Run {} completed successfully", workflowRunId);
        }
    }

    /** Step result record used by executeStep. */
    private record StepExecutionResult(boolean success, boolean suspended, java.time.Instant resumeAt,
                                       String resumeToken, Map<String, Object> outputData) {}

    /**
     * Reads the selected branch key from step output.
     * If/else steps should emit one of: {"_branchKey": "true"} or {"_branchKey": "false"}.
     */
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

    /** DFS to collect all nodes reachable from start via outgoing edges. */
    private void dfsReachable(UUID start, Map<UUID, List<UUID>> outgoing, Set<UUID> visited) {
        if (!visited.add(start)) return;
        for (UUID next : outgoing.getOrDefault(start, List.of())) {
            dfsReachable(next, outgoing, visited);
        }
    }

    /** Kahn's algorithm topological sort over action (non-trigger) steps. */
    private List<UUID> topoSort(Set<UUID> allIds,
                                Map<UUID, List<UUID>> incoming,
                                Map<UUID, List<UUID>> outgoing,
                                Set<UUID> skipSet,
                                UUID triggerId) {
        // Only consider non-trigger, non-skipped nodes
        Set<UUID> actionNodes = new java.util.HashSet<>();
        for (UUID id : allIds) {
            if (!id.equals(triggerId) && !skipSet.contains(id)) actionNodes.add(id);
        }
        if (actionNodes.isEmpty()) return List.of();

        // In-degree count relative to action nodes only
        Map<UUID, Integer> inDegree = new java.util.HashMap<>();
        for (UUID id : actionNodes) {
            int deg = 0;
            for (UUID parent : incoming.getOrDefault(id, List.of())) {
                if (actionNodes.contains(parent) || id.equals(triggerId)) deg++;
                else if (parent.equals(triggerId)) deg++; // trigger counts as parent
            }
            inDegree.put(id, deg);
        }

        // Nodes whose only parents are the trigger (or have in-degree 0 within action set)
        java.util.Queue<UUID> queue = new java.util.ArrayDeque<>();
        for (UUID id : actionNodes) {
            if (inDegree.get(id) == 0) queue.add(id);
            // Also enqueue if all parents are the trigger
            else {
                boolean allParentsTrigger = incoming.getOrDefault(id, List.of()).stream()
                        .allMatch(p -> p.equals(triggerId) || skipSet.contains(p));
                if (allParentsTrigger) queue.add(id);
            }
        }
        // Deduplicate queue
        Set<UUID> queued = new java.util.HashSet<>(queue);

        List<UUID> result = new java.util.ArrayList<>();
        while (!queue.isEmpty()) {
            UUID cur = queue.poll();
            result.add(cur);
            for (UUID child : outgoing.getOrDefault(cur, List.of())) {
                if (!actionNodes.contains(child)) continue;
                inDegree.merge(child, -1, Integer::sum);
                if (inDegree.get(child) <= 0 && !queued.contains(child)) {
                    queue.add(child);
                    queued.add(child);
                }
            }
        }
        return result;
    }



    private void persistExecutionState(UUID userId, UUID workflowRunId,
                                       Map<UUID, Map<String, Object>> allStepOutputs) {
        workflowRunService.saveExecutionState(userId, workflowRunId, stringifiedOutputs(allStepOutputs));
    }

    private Map<String, Object> stringifiedOutputs(Map<UUID, Map<String, Object>> allStepOutputs) {
        Map<String, Object> stringifiedOutputs = new java.util.HashMap<>();
        for (Map.Entry<UUID, Map<String, Object>> entry : allStepOutputs.entrySet()) {
            stringifiedOutputs.put(entry.getKey().toString(), entry.getValue());
        }
        return stringifiedOutputs;
    }

    /**
     * Executes a single step: creates a StepRun, resolves the handler, invokes it,
     * and records the result.
     */
    private StepExecutionResult executeStep(Steps_command step, UUID workflowRunId,
                                            UUID userId, Map<String, Object> inputData,
                                            Map<UUID, Map<String, Object>> allStepOutputs) {

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
                step.getOrder() != null ? step.getOrder().intValue() : 0
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
     * Resolves credentials for a step using a three-tier fallback:
     *
     * <ol>
     *   <li>{@link CredentialSource#PERSONAL} — the user's own connection. Ownership is
     *       enforced at the DB level via {@code findByIdAndUser_Id}.</li>
     *   <li>{@link CredentialSource#PLATFORM} — an admin-configured platform key.
     *       Used as a transparent fallback when the step has no connectionId.
     *       The auth <em>mechanism</em> (OAuth2, APIKEY) does not change between sources;
     *       only <em>whose</em> credential it is differs.</li>
     *   <li>No credential — throws with a user-facing message indicating next steps.</li>
     * </ol>
     *
     * <p>When a connectionId is present, ownership is strictly enforced: a connection
     * owned by another user cannot be used even if the ID is known.
     */
    private Map<String, Object> loadCredentials(UUID connectionId, String appKey, UUID userId) {
        // ── Tier 1: PERSONAL connection (user's own) ──────────────────────────
        if (connectionId != null) {
            Connections_command connection = connectionsRepo
                    .findByIdAndUser_Id(connectionId, userId)
                    .orElse(null);
            if (connection != null) {
                logger.debug("[engine] Using PERSONAL credentials for app '{}' (connection {})",
                        appKey, connectionId);
                return tokenRefreshService.getValidCredentials(connection);
            }
            // Connection ID was set but not found / not owned — hard fail
            logger.error("[engine] Connection {} not found or not owned by user {}",
                    connectionId, userId);
            throw new IllegalStateException(
                    "Connection " + connectionId + " not found or not owned by this user.");
        }

        // ── Tier 2: PLATFORM key (admin-configured fallback) ──────────────────
        boolean isAdmin = userQueryRepo.findById(userId)
                .map(User_query::getRole)
                .map(role -> role == UserRole.ADMIN)
                .orElse(false);

        if (isAdmin) {
            try {
                PlatformKey pk = platformKeyRepo.findByAppKeyAndEnabledTrue(appKey).orElse(null);
                if (pk != null && pk.getEncryptedCredentials() != null) {
                    Map<String, Object> sealed = objectMapper
                            .readValue(pk.getEncryptedCredentials(), Map.class);
                    Map<String, Object> opened = credentialsCryptoService.open(sealed);
                    pk.incrementUsageCount();
                    platformKeyRepo.save(pk);
                    logger.debug("[engine] Using PLATFORM credentials for app '{}' (source=admin)", appKey);
                    return opened;
                }
            } catch (Exception e) {
                logger.warn("[engine] Failed to load platform key for app '{}': {}", appKey, e.getMessage());
            }
        } else {
            logger.debug("[engine] Skipping PLATFORM credentials for app '{}' because user {} is not an ADMIN", appKey, userId);
        }

        // ── Tier 3: No credential available ───────────────────────────────────
        logger.warn("[engine] No credential available for app '{}' (userId={})", appKey, userId);
        throw new IllegalStateException(
                "No credential found for app '" + appKey + "'. " +
                "Connect your own account under Connections, or ask your admin to enable " +
                "the platform key for this app.");
    }

    // UUID-based template pattern: {{steps.<uuid>.fieldName}}
    private static final Pattern TEMPLATE_PATTERN =
            Pattern.compile("\\{\\{steps\\.([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\\.([^}]+)}}");

    // Legacy ordinal pattern: {{steps.N.fieldName}} — kept for backwards compat
    private static final Pattern TEMPLATE_PATTERN_LEGACY = Pattern.compile("\\{\\{steps\\.(\\d+)\\.([^}]+)}}");

    /**
     * Deeply resolves templates in a configuration map.
     * Currently supports string values containing {{steps.N.fieldName}}.
     */
    private Map<String, Object> resolveTemplates(Map<String, Object> config,
                                                   Map<UUID, Map<String, Object>> allStepOutputs) {
        if (config == null || config.isEmpty()) return config;

        Map<String, Object> resolved = new java.util.HashMap<>();
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String strVal) {
                resolved.put(entry.getKey(), resolveStringTemplate(strVal, allStepOutputs));
            } else {
                resolved.put(entry.getKey(), value);
            }
        }
        return resolved;
    }

    private String resolveStringTemplate(String template, Map<UUID, Map<String, Object>> allStepOutputs) {
        if (template == null || !template.contains("{{")) return template;

        // Try UUID-based pattern first
        Matcher matcher = TEMPLATE_PATTERN.matcher(template);
        if (matcher.find()) {
            matcher.reset();
            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                UUID stepId;
                try { stepId = UUID.fromString(matcher.group(1)); }
                catch (IllegalArgumentException e) { continue; }
                String fieldName = matcher.group(2).trim();
                String replacement = "";
                Map<String, Object> stepOut = allStepOutputs.get(stepId);
                if (stepOut != null) {
                    Object val = stepOut.get(fieldName);
                    if (val != null) replacement = String.valueOf(val);
                    else if (fieldName.contains("."))
                        replacement = String.valueOf(resolveNestedProperty(stepOut, fieldName.split("\\.")));
                }
                logger.debug("[engine] Resolved {{steps.{}.{}}} -> {}", stepId, fieldName, replacement);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(sb);
            return sb.toString();
        }

        // Fallback: legacy ordinal pattern {{steps.N.field}} — for workflows not yet migrated
        Matcher legacyMatcher = TEMPLATE_PATTERN_LEGACY.matcher(template);
        if (!legacyMatcher.find()) return template;
        legacyMatcher.reset();
        StringBuilder sb = new StringBuilder();
        while (legacyMatcher.find()) {
            int stepIndex = Integer.parseInt(legacyMatcher.group(1));
            String fieldName = legacyMatcher.group(2).trim();
            // Try to find by matching step order against outputs
            String replacement = "";
            for (Map.Entry<UUID, Map<String, Object>> entry : allStepOutputs.entrySet()) {
                // Legacy fallback: match by ordinal position in sorted order (best effort)
                // For newly created workflows this path should never fire
                Map<String, Object> stepOut = entry.getValue();
                if (stepOut != null) {
                    Object val = stepOut.get(fieldName);
                    if (val != null) { replacement = String.valueOf(val); break; }
                }
            }
            logger.debug("[engine] Legacy resolved {{steps.{}.{}}} -> {}", stepIndex, fieldName, replacement);
            legacyMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        legacyMatcher.appendTail(sb);
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
