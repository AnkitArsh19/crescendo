package com.crescendo.execution.agent;

import com.crescendo.app.App;
import com.crescendo.app.AppRepository;
import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionHandlerRegistry;
import com.crescendo.execution.action.ActionResult;
import com.crescendo.execution.engine.WorkflowExecutionEngine;
import com.crescendo.shared.domain.valueobject.AppKey;
import com.crescendo.steps.steps_command.Steps_command;
import com.crescendo.steps.steps_command.Steps_commandRepository;
import com.crescendo.user.user_query.User_queryRepository;
import com.crescendo.enums.UserRole;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Orchestrates the ReAct (Reason → Act → Observe) loop for Agentic AI Cluster Nodes.
 *
 * <h2>Execution Contract</h2>
 * <ul>
 *   <li>Java owns: tool dispatch, credential resolution, output sanitisation,
 *       per-iteration budget check, and checkpoint logging.</li>
 *   <li>Python owns: one LLM call per iteration, returning a typed decision
 *       (tool_call | final_answer) via {@code POST /v1/agent/next-step}.</li>
 *   <li>Native fallback: when Python is offline, Java directly calls Gemini or
 *       OpenAI/Groq with function calling definitions and handles turns.</li>
 * </ul>
 */
@Service
public class AgentExecutionService {

    private static final Logger log = LoggerFactory.getLogger(AgentExecutionService.class);

    // ── Known prompt-injection patterns to strip from tool output ─────────
    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("(?i)ignore\\s+(all\\s+)?(previous|above|prior)\\s+instructions?"),
            Pattern.compile("(?i)disregard\\s+(all\\s+)?(previous|above|prior)\\s+instructions?"),
            Pattern.compile("(?i)forget\\s+(all\\s+)?(previous|above|prior)\\s+instructions?"),
            Pattern.compile("(?i)you\\s+are\\s+now\\s+(a|an|the)\\b"),
            Pattern.compile("(?i)act\\s+as\\s+(a|an|the)\\b"),
            Pattern.compile("(?i)reveal\\s+(your\\s+)?(system\\s+prompt|instructions?)"),
            Pattern.compile("(?i)</?\\s*(system|instructions?|prompt)\\s*>")
    );

    private final SubWorkflowToolRunner subWorkflowToolRunner;
    private final ActionHandlerRegistry actionHandlerRegistry;
    private final WorkflowExecutionEngine workflowExecutionEngine;
    private final AppRepository appRepo;
    private final Steps_commandRepository stepsRepo;
    private final User_queryRepository userQueryRepo;
    private final ObjectMapper objectMapper;
    private final String pythonBaseUrl;
    private final String pythonServiceToken;
    private final String platformGeminiApiKey;

    public AgentExecutionService(
            SubWorkflowToolRunner subWorkflowToolRunner,
            @Lazy ActionHandlerRegistry actionHandlerRegistry,
            @Lazy WorkflowExecutionEngine workflowExecutionEngine,
            AppRepository appRepo,
            Steps_commandRepository stepsRepo,
            User_queryRepository userQueryRepo,
            ObjectMapper objectMapper,
            @Value("${crescendo.python-ai.base-url:}") String pythonBaseUrl,
            @Value("${crescendo.python-ai.service-token:}") String pythonServiceToken,
            @Value("${gemini.api.key:}") String platformGeminiApiKey
    ) {
        this.subWorkflowToolRunner = subWorkflowToolRunner;
        this.actionHandlerRegistry = actionHandlerRegistry;
        this.workflowExecutionEngine = workflowExecutionEngine;
        this.appRepo = appRepo;
        this.stepsRepo = stepsRepo;
        this.userQueryRepo = userQueryRepo;
        this.objectMapper = objectMapper;
        this.pythonBaseUrl = pythonBaseUrl;
        this.pythonServiceToken = pythonServiceToken;
        this.platformGeminiApiKey = platformGeminiApiKey;
    }

    /**
     * Internal metadata for a tool resolved at execution time.
     */
    public record ResolvedTool(
            String toolId,
            String appKey,
            String actionKey,
            UUID connectionId,
            UUID subWorkflowId,
            Map<String, Object> fixedParams,
            AgentNextStepRequest.ToolDefinition definition
    ) {}

    // ─────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────

    public Map<String, Object> executeAgentLoop(
            UUID workflowRunId,
            UUID ownerUserId,
            AgentClusterConfig config,
            String systemPrompt,
            Map<String, Object> executionContext,
            String provider,
            String model,
            String userApiKey
    ) {
        String effectiveProvider = (provider != null && !provider.isBlank()) ? provider.trim().toLowerCase() : "gemini";
        String effectiveModel = (model != null && !model.isBlank()) ? model.trim() : ("gemini".equals(effectiveProvider) ? "gemini-3.5-flash-lite" : "gpt-4o");
        if ("gemini".equals(effectiveProvider)) {
            effectiveModel = normalizeGeminiModel(effectiveModel);
        }

        String effectiveApiKey = (userApiKey != null && !userApiKey.isBlank() && !"null".equalsIgnoreCase(userApiKey)) ? userApiKey.trim() : null;
        boolean usingPlatformKey = false;
        if (effectiveApiKey == null && "gemini".equals(effectiveProvider)) {
            effectiveApiKey = platformGeminiApiKey;
            usingPlatformKey = true;
        }

        boolean isAdmin = false;
        if (ownerUserId != null && userQueryRepo != null) {
            isAdmin = userQueryRepo.findById(ownerUserId)
                    .map(u -> u.getRole() == UserRole.ADMIN)
                    .orElse(false);
        }

        // Platform key model authorization:
        // Admin users have full access to all models.
        // Non-admin users are restricted to gemini-3.5-flash-lite free tier when using platform keys.
        if (usingPlatformKey && !isAdmin) {
            if (!"gemini".equals(effectiveProvider) || !"gemini-3.5-flash-lite".equalsIgnoreCase(effectiveModel)) {
                log.info("Non-admin user {} attempted to use provider={} model={} with platform key; falling back to gemini:gemini-3.5-flash-lite",
                        ownerUserId, effectiveProvider, effectiveModel);
                effectiveProvider = "gemini";
                effectiveModel = "gemini-3.5-flash-lite";
            }
        }

        if (effectiveApiKey == null || effectiveApiKey.isBlank()) {
            log.warn("Agent execution failed: No API key available for provider={}", effectiveProvider);
            return Map.of(
                    "status", "AUTH_REQUIRED",
                    "error", "API key is required for provider '" + effectiveProvider + "'. Please attach a connection or configure gemini.api.key in application.properties."
            );
        }

        // Build dynamic tool definitions and resolved tools lookup
        Map<String, ResolvedTool> resolvedTools = buildResolvedTools(config, ownerUserId);
        List<AgentNextStepRequest.ToolDefinition> toolDefs = resolvedTools.values().stream()
                .map(ResolvedTool::definition)
                .toList();

        log.info("Starting Agent Execution Loop: workflowRunId={} owner={} provider={} model={} maxIterations={} tokenBudget={} toolsCount={}",
                workflowRunId, ownerUserId, effectiveProvider, effectiveModel, config.maxIterations(), config.tokenBudget(), toolDefs.size());

        String agentSessionId = "agent-run:" + workflowRunId;
        List<AgentNextStepRequest.ConversationTurn> history = new ArrayList<>();
        List<Map<String, Object>> timeline = new ArrayList<>();

        Map<String, Object> initialTurn = new LinkedHashMap<>();
        initialTurn.put("turn", 0);
        initialTurn.put("role", "user");
        initialTurn.put("type", "input");
        initialTurn.put("content", executionContext != null && !executionContext.isEmpty() ? safeJson(executionContext) : ("Instructions: " + systemPrompt));
        timeline.add(initialTurn);

        if (executionContext != null && !executionContext.isEmpty()) {
            String inputJson = safeJson(executionContext);
            history.add(new AgentNextStepRequest.ConversationTurn("user", inputJson, null));
        }
        if (history.isEmpty()) {
            history.add(new AgentNextStepRequest.ConversationTurn("user", "Instructions: " + systemPrompt, null));
        }

        int accumulatedTokens = 0;

        for (int iteration = 1; iteration <= config.maxIterations(); iteration++) {

            // 1. Pre-execution budget check
            if (accumulatedTokens >= config.tokenBudget()) {
                log.warn("Agent run={} aborted: token budget ({}) reached before iteration {}",
                        agentSessionId, config.tokenBudget(), iteration);
                Map<String, Object> budgetMap = new HashMap<>();
                budgetMap.put("status", "BUDGET_EXCEEDED");
                budgetMap.put("iterations", iteration - 1);
                budgetMap.put("tokensUsed", accumulatedTokens);
                budgetMap.put("error", "Token budget exhausted before iteration " + iteration);
                budgetMap.put("timeline", timeline);
                return budgetMap;
            }

            log.info("Agent turn {}/{} | session={} | tokensUsed={}/{}",
                    iteration, config.maxIterations(), agentSessionId, accumulatedTokens, config.tokenBudget());

            // 2. Call AI Reasoning Model
            AgentNextStepResponse response;
            try {
                if (pythonBaseUrl != null && !pythonBaseUrl.isBlank()) {
                    response = callPythonNextStep(agentSessionId, iteration, systemPrompt, toolDefs, history, executionContext, effectiveProvider, effectiveModel, effectiveApiKey);
                } else if ("gemini".equals(effectiveProvider)) {
                    response = callDirectGemini(effectiveApiKey, effectiveModel, systemPrompt, toolDefs, history, executionContext, resolvedTools);
                } else {
                    response = callDirectOpenAICompatible(effectiveProvider, effectiveApiKey, effectiveModel, systemPrompt, toolDefs, history, executionContext, resolvedTools);
                }
            } catch (Exception e) {
                log.error("Agent turn {}: AI reasoning call failed — {}", iteration, e.getMessage(), e);
                // Fallback to direct Gemini/OpenAI if Python was configured but failed
                if (pythonBaseUrl != null && !pythonBaseUrl.isBlank()) {
                    try {
                        log.info("Attempting direct native fallback for provider={}", effectiveProvider);
                        if ("gemini".equals(effectiveProvider)) {
                            response = callDirectGemini(effectiveApiKey, effectiveModel, systemPrompt, toolDefs, history, executionContext, resolvedTools);
                        } else {
                            response = callDirectOpenAICompatible(effectiveProvider, effectiveApiKey, effectiveModel, systemPrompt, toolDefs, history, executionContext, resolvedTools);
                        }
                    } catch (Exception directEx) {
                        Map<String, Object> errMap = new HashMap<>();
                        errMap.put("status", "AI_SERVICE_ERROR");
                        errMap.put("iterations", iteration);
                        errMap.put("tokensUsed", accumulatedTokens);
                        errMap.put("error", "AI reasoning service failed: " + directEx.getMessage());
                        errMap.put("timeline", timeline);
                        return errMap;
                    }
                } else {
                    Map<String, Object> errMap = new HashMap<>();
                    errMap.put("status", "AI_SERVICE_ERROR");
                    errMap.put("iterations", iteration);
                    errMap.put("tokensUsed", accumulatedTokens);
                    errMap.put("error", "AI reasoning service failed: " + e.getMessage());
                    errMap.put("timeline", timeline);
                    return errMap;
                }
            }

            accumulatedTokens += response.tokensUsed();

            if (response.reasoning() != null && !response.reasoning().isBlank()) {
                Map<String, Object> thoughtTurn = new LinkedHashMap<>();
                thoughtTurn.put("turn", iteration);
                thoughtTurn.put("role", "assistant");
                thoughtTurn.put("type", "thought");
                thoughtTurn.put("thought", response.reasoning());
                timeline.add(thoughtTurn);
            }

            // 3a. Final answer — agent is done
            if (response.isFinalAnswer()) {
                log.info("Agent run={} completed with final answer after {} iterations (tokensUsed={})",
                        agentSessionId, iteration, accumulatedTokens);
                String answer = response.finalAnswer() != null ? response.finalAnswer() : "";

                Map<String, Object> finalAnswerTurn = new LinkedHashMap<>();
                finalAnswerTurn.put("turn", iteration);
                finalAnswerTurn.put("role", "assistant");
                finalAnswerTurn.put("type", "final_answer");
                finalAnswerTurn.put("answer", answer);
                timeline.add(finalAnswerTurn);

                Map<String, Object> finalMap = new HashMap<>();
                finalMap.put("status", "COMPLETED");
                finalMap.put("iterations", iteration);
                finalMap.put("tokensUsed", accumulatedTokens);
                finalMap.put("result", answer);
                finalMap.put("finalAnswer", answer);
                finalMap.put("output", answer);
                finalMap.put("text", answer);
                finalMap.put("timeline", timeline);
                return finalMap;
            }

            // 3b. Tool call — dispatch through registry or sub-workflow runner
            if (response.isToolCall()) {
                AgentNextStepResponse.ToolCallDecision toolCall = response.toolCall();
                String idempotencyKey = String.format("agent-tool:%s:%d", workflowRunId, iteration);

                log.info("Agent turn {}: calling tool toolId={} appKey={} actionKey={} args={}",
                        iteration, toolCall.toolId(), toolCall.appKey(), toolCall.actionKey(), toolCall.arguments());

                Map<String, Object> toolCallTurn = new LinkedHashMap<>();
                toolCallTurn.put("turn", iteration);
                toolCallTurn.put("role", "assistant");
                toolCallTurn.put("type", "tool_call");
                toolCallTurn.put("toolId", toolCall.toolId());
                toolCallTurn.put("appKey", toolCall.appKey());
                toolCallTurn.put("actionKey", toolCall.actionKey());
                toolCallTurn.put("arguments", toolCall.arguments());
                timeline.add(toolCallTurn);

                // Append assistant's decision to history with Phase 2 fields
                history.add(new AgentNextStepRequest.ConversationTurn(
                        "assistant",
                        "Calling tool: " + toolCall.actionKey() + " with " + safeJson(toolCall.arguments()),
                        idempotencyKey,
                        toolCall.toolId(),
                        safeJson(toolCall.arguments())
                ));

                // Dispatch tool
                Map<String, Object> toolOutput;
                try {
                    toolOutput = dispatchTool(toolCall, resolvedTools, ownerUserId, workflowRunId, idempotencyKey);
                } catch (Exception e) {
                    log.error("Agent turn {}: tool dispatch failed for {} — {}", iteration, toolCall.actionKey(), e.getMessage(), e);
                    toolOutput = Map.of("error", "Tool execution failed: " + e.getMessage(), "status", "FAILURE");
                }

                Map<String, Object> obsTurn = new LinkedHashMap<>();
                obsTurn.put("turn", iteration);
                obsTurn.put("role", "tool");
                obsTurn.put("type", "observation");
                obsTurn.put("toolId", toolCall.toolId());
                obsTurn.put("actionKey", toolCall.actionKey());
                obsTurn.put("output", toolOutput);
                timeline.add(obsTurn);

                // Sanitise tool output before feeding back to LLM
                String sanitisedObservation = sanitiseToolOutput(toolOutput);

                // Append tool observation to history
                history.add(new AgentNextStepRequest.ConversationTurn("tool", sanitisedObservation, idempotencyKey));

                log.info("Agent turn {} checkpoint: tool={} output_length={} tokensUsed={}",
                        iteration, toolCall.actionKey(), sanitisedObservation.length(), accumulatedTokens);
            }
        }

        log.warn("Agent run={} reached maxIterations ({}) without a final answer.", agentSessionId, config.maxIterations());
        Map<String, Object> timeoutMap = new HashMap<>();
        timeoutMap.put("status", "MAX_ITERATIONS_REACHED");
        timeoutMap.put("iterations", config.maxIterations());
        timeoutMap.put("tokensUsed", accumulatedTokens);
        timeoutMap.put("error", "Agent did not produce a final answer within " + config.maxIterations() + " iterations");
        timeoutMap.put("timeline", timeline);
        return timeoutMap;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Tool Resolution and Schema Synthesis
    // ─────────────────────────────────────────────────────────────────────

    private Map<String, ResolvedTool> buildResolvedTools(AgentClusterConfig config, UUID ownerUserId) {
        Map<String, ResolvedTool> map = new LinkedHashMap<>();

        // 1. Process explicitly configured tools
        if (config.configuredTools() != null) {
            for (AgentClusterConfig.ToolConfig tc : config.configuredTools()) {
                if (tc.subWorkflowId() != null || "workflow".equalsIgnoreCase(tc.appKey())) {
                    UUID subWfId = tc.subWorkflowId();
                    String toolId = sanitizeToolId(tc.toolId() != null && !tc.toolId().isBlank() ?
                            tc.toolId() : ("subworkflow_" + (subWfId != null ? subWfId.toString().replace("-", "_") : "custom")));
                    String desc = tc.customDescription() != null && !tc.customDescription().isBlank() ?
                            tc.customDescription() : "Executes sub-workflow " + subWfId + " and returns its structured output.";
                    Map<String, Object> params = Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "workflowId", Map.of("type", "string", "description", "Sub-workflow UUID"),
                                    "payload", Map.of("type", "object", "description", "Input payload parameters for the sub-workflow")
                            ),
                            "required", List.of("workflowId")
                    );
                    AgentNextStepRequest.ToolDefinition td = new AgentNextStepRequest.ToolDefinition(
                            toolId, "workflow", "run_subworkflow", desc, params
                    );
                    map.put(toolId, new ResolvedTool(toolId, "workflow", "run_subworkflow", tc.connectionId(), subWfId, tc.fixedParameters(), td));
                } else if (tc.appKey() != null && tc.actionKey() != null) {
                    String appKey = tc.appKey().trim();
                    String actionKey = tc.actionKey().trim();
                    String toolId = sanitizeToolId(tc.toolId() != null && !tc.toolId().isBlank() ?
                            tc.toolId() : (appKey + "__" + actionKey));

                    String desc = tc.customDescription();
                    Map<String, Object> schema = Map.of("type", "object", "properties", Map.of());

                    if (appRepo != null) {
                        try {
                            var appOpt = appRepo.findById(AppKey.of(appKey));
                            if (appOpt.isPresent() && appOpt.get().getActions() != null) {
                                for (Map<String, Object> action : appOpt.get().getActions()) {
                                    String actKey = String.valueOf(action.getOrDefault("actionKey", action.getOrDefault("key", "")));
                                    if (actKey.equalsIgnoreCase(actionKey) || normalizeKey(actKey).equalsIgnoreCase(normalizeKey(actionKey))) {
                                        if (desc == null || desc.isBlank()) {
                                            desc = String.valueOf(action.getOrDefault("description", action.getOrDefault("name", actionKey)));
                                        }
                                        if (action.get("configSchema") instanceof List<?> rawSchema) {
                                            @SuppressWarnings("unchecked")
                                            List<Map<String, Object>> typedSchema = (List<Map<String, Object>>) rawSchema;
                                            schema = buildJsonSchemaForAction(typedSchema);
                                        }
                                        break;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.debug("Could not inspect app metadata for tool {}:{}: {}", appKey, actionKey, e.getMessage());
                        }
                    }

                    if (desc == null || desc.isBlank()) {
                        desc = "Executes the " + actionKey + " action of " + appKey;
                    }

                    AgentNextStepRequest.ToolDefinition td = new AgentNextStepRequest.ToolDefinition(
                            toolId, appKey, actionKey, desc, schema
                    );
                    map.put(toolId, new ResolvedTool(toolId, appKey, actionKey, tc.connectionId(), null, tc.fixedParameters(), td));
                }
            }
        }

        // 2. Process toolRefs (step UUID references from canvas)
        if (config.toolRefs() != null && stepsRepo != null) {
            for (UUID stepId : config.toolRefs()) {
                try {
                    stepsRepo.findById(stepId).ifPresent(step -> {
                        String appKey = step.getAppKey();
                        String actionKey = step.getActionKey();
                        if (appKey != null && actionKey != null) {
                            String toolId = sanitizeToolId(appKey + "__" + actionKey + "_" + stepId.toString().substring(0, 8));
                            if (!map.containsKey(toolId)) {
                                String desc = step.getName() != null ? step.getName() : ("Step " + actionKey + " of " + appKey);
                                Map<String, Object> schema = Map.of("type", "object", "properties", Map.of());
                                if (appRepo != null) {
                                    try {
                                        var appOpt = appRepo.findById(AppKey.of(appKey));
                                        if (appOpt.isPresent() && appOpt.get().getActions() != null) {
                                            for (Map<String, Object> action : appOpt.get().getActions()) {
                                                String actKey = String.valueOf(action.getOrDefault("actionKey", action.getOrDefault("key", "")));
                                                if (actKey.equalsIgnoreCase(actionKey) || normalizeKey(actKey).equalsIgnoreCase(normalizeKey(actionKey))) {
                                                    if (action.get("configSchema") instanceof List<?> rawSchema) {
                                                        @SuppressWarnings("unchecked")
                                                        List<Map<String, Object>> typedSchema = (List<Map<String, Object>>) rawSchema;
                                                        schema = buildJsonSchemaForAction(typedSchema);
                                                    }
                                                    break;
                                                }
                                            }
                                        }
                                    } catch (Exception ignored) {}
                                }
                                AgentNextStepRequest.ToolDefinition td = new AgentNextStepRequest.ToolDefinition(
                                        toolId, appKey, actionKey, desc, schema
                                );
                                map.put(toolId, new ResolvedTool(toolId, appKey, actionKey, step.getConnectionId(), null, step.getConfiguration(), td));
                            }
                        }
                    });
                } catch (Exception e) {
                    log.debug("Could not resolve step toolRef {}: {}", stepId, e.getMessage());
                }
            }
        }

        return map;
    }

    private Map<String, Object> buildJsonSchemaForAction(List<Map<String, Object>> configSchema) {
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        if (configSchema != null) {
            for (Map<String, Object> field : configSchema) {
                String key = String.valueOf(field.getOrDefault("key", ""));
                if (key.isBlank()) continue;

                String label = String.valueOf(field.getOrDefault("label", key));
                String type = String.valueOf(field.getOrDefault("type", "text")).toLowerCase();
                boolean isRequired = Boolean.parseBoolean(String.valueOf(field.getOrDefault("required", "false")));

                String jsonType = switch (type) {
                    case "number", "integer" -> "number";
                    case "boolean", "checkbox", "toggle" -> "boolean";
                    case "json", "object" -> "object";
                    case "list", "array" -> "array";
                    default -> "string";
                };

                Map<String, Object> propDef = new LinkedHashMap<>();
                propDef.put("type", jsonType);
                String helpText = field.get("helpText") != null ? String.valueOf(field.get("helpText")) : null;
                String desc = (helpText != null && !helpText.isBlank()) ? (label + " (" + helpText + ")") : label;
                propDef.put("description", desc);

                properties.put(key, propDef);
                if (isRequired) {
                    required.add(key);
                }
            }
        }

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    private String sanitizeToolId(String raw) {
        if (raw == null || raw.isBlank()) {
            return "t_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        }
        String s = raw.replaceAll("[^a-zA-Z0-9_]", "_");
        if (!s.matches("^[a-zA-Z_].*")) {
            s = "t_" + s;
        }
        if (s.length() > 64) {
            s = s.substring(0, 64);
        }
        return s;
    }

    private String normalizeKey(String key) {
        return key == null ? "" : key.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Private Helpers: Remote & Native Reasoning Calls
    // ─────────────────────────────────────────────────────────────────────

    private AgentNextStepResponse callPythonNextStep(
            String sessionId,
            int iteration,
            String systemPrompt,
            List<AgentNextStepRequest.ToolDefinition> toolDefs,
            List<AgentNextStepRequest.ConversationTurn> history,
            Map<String, Object> inputData,
            String provider,
            String model,
            String apiKey
    ) {
        AgentNextStepRequest requestBody = new AgentNextStepRequest(
                sessionId, iteration, systemPrompt, toolDefs != null ? toolDefs : List.of(), history, inputData, provider, model, apiKey
        );

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(trimTrailingSlash(pythonBaseUrl))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        if (pythonServiceToken != null && !pythonServiceToken.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + pythonServiceToken);
        }

        return builder.build()
                .post()
                .uri("/v1/agent/next-step")
                .body(requestBody)
                .retrieve()
                .body(AgentNextStepResponse.class);
    }

    private String normalizeGeminiModel(String model) {
        if (model == null || model.isBlank()) {
            return "gemini-3.5-flash-lite";
        }
        String clean = model.trim();
        if (clean.startsWith("models/")) {
            clean = clean.substring("models/".length());
        }
        // Fallback for legacy models
        if (clean.toLowerCase().startsWith("gemma")) {
            log.warn("Model '{}' does not support dynamic function calling in Gemini API. Automatically falling back to default 'gemini-3.5-flash-lite'.", clean);
            return "gemini-3.5-flash-lite";
        }
        if ("gemini-2.5-flash".equalsIgnoreCase(clean) || "gemini-1.5-flash".equalsIgnoreCase(clean) || "gemini-flash".equalsIgnoreCase(clean)) {
            return "gemini-3.5-flash-lite";
        }
        if ("gemini-pro".equalsIgnoreCase(clean) || "gemini-pro-latest".equalsIgnoreCase(clean) || "gemini-1.5-pro".equalsIgnoreCase(clean)) {
            return "gemini-3.8-flash";
        }
        return clean;
    }

    @SuppressWarnings("unchecked")
    private AgentNextStepResponse callDirectGemini(
            String apiKey,
            String model,
            String systemPrompt,
            List<AgentNextStepRequest.ToolDefinition> toolDefs,
            List<AgentNextStepRequest.ConversationTurn> history,
            Map<String, Object> inputData,
            Map<String, ResolvedTool> resolvedTools
    ) {
        String effectiveModel = normalizeGeminiModel(model);
        RestClient client = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        String endpoint = "/" + effectiveModel + ":generateContent?key=" + apiKey;

        List<Map<String, Object>> contents = new ArrayList<>();
        for (AgentNextStepRequest.ConversationTurn turn : history) {
            if ("tool".equalsIgnoreCase(turn.role())) {
                contents.add(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", "Observation from tool [" + (turn.toolName() != null ? turn.toolName() : "tool") + "]:\n" + (turn.content() != null ? turn.content() : "")))
                ));
            } else if ("assistant".equalsIgnoreCase(turn.role())) {
                if (turn.toolName() != null && !turn.toolName().isBlank()) {
                    Map<String, Object> argsMap = Map.of();
                    try {
                        if (turn.toolArgsJson() != null) {
                            argsMap = objectMapper.readValue(turn.toolArgsJson(), Map.class);
                        }
                    } catch (Exception ignored) {}
                    contents.add(Map.of(
                            "role", "model",
                            "parts", List.of(Map.of("functionCall", Map.of("name", turn.toolName(), "args", argsMap)))
                    ));
                } else {
                    contents.add(Map.of(
                            "role", "model",
                            "parts", List.of(Map.of("text", turn.content() != null ? turn.content() : ""))
                    ));
                }
            } else {
                contents.add(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", turn.content() != null ? turn.content() : ""))
                ));
            }
        }

        if (contents.isEmpty() && inputData != null && !inputData.isEmpty()) {
            contents.add(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", safeJson(inputData)))
            ));
        }

        Map<String, Object> body = new HashMap<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            body.put("system_instruction", Map.of("parts", List.of(Map.of("text", systemPrompt))));
        }
        body.put("contents", contents);

        // Attach Gemini function declarations if tools exist
        if (toolDefs != null && !toolDefs.isEmpty()) {
            List<Map<String, Object>> functionDeclarations = new ArrayList<>();
            for (AgentNextStepRequest.ToolDefinition td : toolDefs) {
                Map<String, Object> decl = new HashMap<>();
                decl.put("name", td.toolId());
                decl.put("description", td.description());
                if (td.parameters() != null && !td.parameters().isEmpty()) {
                    decl.put("parameters", td.parameters());
                }
                functionDeclarations.add(decl);
            }
            body.put("tools", List.of(Map.of("function_declarations", functionDeclarations)));
        }

        try {
            String response = client.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> parsed = objectMapper.readValue(response, Map.class);
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) parsed.get("candidates");
            String text = "";
            int tokensUsed = 0;
            Map<String, Object> usage = (Map<String, Object>) parsed.get("usageMetadata");
            if (usage != null && usage.get("totalTokenCount") instanceof Number n) {
                tokensUsed = n.intValue();
            }

            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                if (content != null) {
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        Map<String, Object> firstPart = parts.get(0);
                        if (firstPart.containsKey("functionCall")) {
                            Map<String, Object> fc = (Map<String, Object>) firstPart.get("functionCall");
                            String funcName = String.valueOf(fc.get("name"));
                            Map<String, Object> args = (Map<String, Object>) fc.getOrDefault("args", Map.of());

                            ResolvedTool resolved = resolvedTools.get(funcName);
                            String appKey = resolved != null ? resolved.appKey() : "unknown";
                            String actionKey = resolved != null ? resolved.actionKey() : funcName;

                            AgentNextStepResponse.ToolCallDecision decision = new AgentNextStepResponse.ToolCallDecision(
                                    funcName, appKey, actionKey, args
                            );
                            return new AgentNextStepResponse("tool_call", decision, null, null, tokensUsed);
                        }
                        text = String.valueOf(firstPart.getOrDefault("text", ""));
                    }
                }
            }

            return new AgentNextStepResponse("final_answer", null, text, null, tokensUsed);
        } catch (Exception e) {
            log.error("Direct Gemini reasoning failed: {}", e.getMessage(), e);
            throw new RestClientException("Direct Gemini reasoning failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private AgentNextStepResponse callDirectOpenAICompatible(
            String provider,
            String apiKey,
            String model,
            String systemPrompt,
            List<AgentNextStepRequest.ToolDefinition> toolDefs,
            List<AgentNextStepRequest.ConversationTurn> history,
            Map<String, Object> inputData,
            Map<String, ResolvedTool> resolvedTools
    ) {
        String baseUrl = "groq".equalsIgnoreCase(provider)
                ? "https://api.groq.com/openai/v1"
                : "https://api.openai.com/v1";

        RestClient client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        List<Map<String, Object>> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        for (AgentNextStepRequest.ConversationTurn turn : history) {
            if ("assistant".equalsIgnoreCase(turn.role()) && turn.toolName() != null) {
                Map<String, Object> asstMsg = new HashMap<>();
                asstMsg.put("role", "assistant");
                asstMsg.put("content", turn.content() != null ? turn.content() : "");
                asstMsg.put("tool_calls", List.of(Map.of(
                        "id", turn.toolCallId() != null ? turn.toolCallId() : "call_" + UUID.randomUUID().toString().substring(0, 8),
                        "type", "function",
                        "function", Map.of(
                                "name", turn.toolName(),
                                "arguments", turn.toolArgsJson() != null ? turn.toolArgsJson() : "{}"
                        )
                )));
                messages.add(asstMsg);
            } else if ("tool".equalsIgnoreCase(turn.role())) {
                Map<String, Object> toolMsg = new HashMap<>();
                toolMsg.put("role", "tool");
                toolMsg.put("tool_call_id", turn.toolCallId() != null ? turn.toolCallId() : "");
                toolMsg.put("content", turn.content() != null ? turn.content() : "");
                messages.add(toolMsg);
            } else {
                messages.add(Map.of("role", turn.role(), "content", turn.content() != null ? turn.content() : ""));
            }
        }
        if (messages.isEmpty() && inputData != null && !inputData.isEmpty()) {
            messages.add(Map.of("role", "user", "content", safeJson(inputData)));
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);

        if (toolDefs != null && !toolDefs.isEmpty()) {
            List<Map<String, Object>> openAiTools = new ArrayList<>();
            for (AgentNextStepRequest.ToolDefinition td : toolDefs) {
                openAiTools.add(Map.of(
                        "type", "function",
                        "function", Map.of(
                                "name", td.toolId(),
                                "description", td.description(),
                                "parameters", td.parameters() != null ? td.parameters() : Map.of("type", "object", "properties", Map.of())
                        )
                ));
            }
            body.put("tools", openAiTools);
            body.put("tool_choice", "auto");
        }

        try {
            String response = client.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> parsed = objectMapper.readValue(response, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) parsed.get("choices");
            String text = "";
            int tokensUsed = 0;
            Map<String, Object> usage = (Map<String, Object>) parsed.get("usage");
            if (usage != null && usage.get("total_tokens") instanceof Number n) {
                tokensUsed = n.intValue();
            }

            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                if (message != null) {
                    List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) message.get("tool_calls");
                    if (toolCalls != null && !toolCalls.isEmpty()) {
                        Map<String, Object> tc = toolCalls.get(0);
                        String tcId = String.valueOf(tc.getOrDefault("id", "call_" + UUID.randomUUID().toString().substring(0, 8)));
                        Map<String, Object> fn = (Map<String, Object>) tc.get("function");
                        String funcName = String.valueOf(fn.get("name"));
                        String argsStr = String.valueOf(fn.getOrDefault("arguments", "{}"));
                        Map<String, Object> args = Map.of();
                        try {
                            args = objectMapper.readValue(argsStr, Map.class);
                        } catch (Exception ignored) {}

                        ResolvedTool resolved = resolvedTools.get(funcName);
                        String appKey = resolved != null ? resolved.appKey() : "unknown";
                        String actionKey = resolved != null ? resolved.actionKey() : funcName;

                        AgentNextStepResponse.ToolCallDecision decision = new AgentNextStepResponse.ToolCallDecision(
                                funcName, appKey, actionKey, args
                        );
                        return new AgentNextStepResponse("tool_call", decision, null, null, tokensUsed);
                    }
                    text = String.valueOf(message.getOrDefault("content", ""));
                }
            }

            return new AgentNextStepResponse("final_answer", null, text, null, tokensUsed);
        } catch (Exception e) {
            log.error("Direct {} reasoning failed: {}", provider, e.getMessage(), e);
            throw new RestClientException("Direct " + provider + " reasoning failed: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Tool Dispatch with Credential Resolution
    // ─────────────────────────────────────────────────────────────────────

    private Map<String, Object> dispatchTool(
            AgentNextStepResponse.ToolCallDecision toolCall,
            Map<String, ResolvedTool> resolvedTools,
            UUID ownerUserId,
            UUID workflowRunId,
            String idempotencyKey
    ) {
        ResolvedTool resolvedTool = resolvedTools.get(toolCall.toolId());
        if (resolvedTool == null) {
            // Match by appKey and actionKey
            for (ResolvedTool rt : resolvedTools.values()) {
                if (rt.appKey().equalsIgnoreCase(toolCall.appKey()) &&
                        (rt.actionKey().equalsIgnoreCase(toolCall.actionKey()) ||
                                normalizeKey(rt.actionKey()).equalsIgnoreCase(normalizeKey(toolCall.actionKey())))) {
                    resolvedTool = rt;
                    break;
                }
            }
        }

        // 1. Sub-workflow tool execution
        if ((resolvedTool != null && resolvedTool.subWorkflowId() != null) || "workflow".equalsIgnoreCase(toolCall.appKey())) {
            UUID subWfId = resolvedTool != null ? resolvedTool.subWorkflowId() : null;
            if (subWfId == null) {
                String idStr = String.valueOf(toolCall.arguments().getOrDefault("workflowId", ""));
                if (!idStr.isBlank()) {
                    try {
                        subWfId = UUID.fromString(idStr);
                    } catch (Exception ignored) {}
                }
            }
            if (subWfId != null) {
                return subWorkflowToolRunner.executeSubWorkflowTool(subWfId, ownerUserId, toolCall.arguments());
            }
        }

        // 2. Standard app catalog tool execution
        String appKey = resolvedTool != null ? resolvedTool.appKey() : toolCall.appKey();
        String actionKey = resolvedTool != null ? resolvedTool.actionKey() : toolCall.actionKey();
        UUID connectionId = resolvedTool != null ? resolvedTool.connectionId() : null;

        // Handler lookup with camelCase / snake_case tolerance
        Optional<ActionHandler> handlerOpt = actionHandlerRegistry.find(appKey, actionKey);
        if (handlerOpt.isEmpty()) {
            String altKey = actionKey.contains("_") ? snakeToCamel(actionKey) : camelToSnake(actionKey);
            handlerOpt = actionHandlerRegistry.find(appKey, altKey);
        }

        if (handlerOpt.isEmpty()) {
            log.warn("No handler found for tool appKey={} actionKey={}", appKey, actionKey);
            return Map.of("error", "Unknown tool: " + actionKey + " for app " + appKey, "status", "FAILURE");
        }

        // Resolve credentials from workflow execution engine
        Map<String, Object> credentials = Map.of();
        if (workflowExecutionEngine != null) {
            try {
                credentials = workflowExecutionEngine.loadCredentials(connectionId, appKey, ownerUserId);
            } catch (Exception e) {
                log.warn("Credential resolution failed for tool appKey={} connectionId={}: {}", appKey, connectionId, e.getMessage());
            }
        }

        // Merge fixed parameters with tool call arguments
        Map<String, Object> effectiveConfig = new HashMap<>();
        if (resolvedTool != null && resolvedTool.fixedParams() != null) {
            effectiveConfig.putAll(resolvedTool.fixedParams());
        }
        if (toolCall.arguments() != null) {
            effectiveConfig.putAll(toolCall.arguments());
        }

        ActionContext toolContext = new ActionContext(
                appKey,
                actionKey,
                effectiveConfig,
                credentials,
                Map.of(),
                workflowRunId,
                ownerUserId,
                UUID.randomUUID(),
                0
        );

        ActionResult result = handlerOpt.get().execute(toolContext);
        if (!result.success()) {
            return Map.of("error", result.error() != null ? result.error() : "Action execution failed", "status", "FAILURE");
        }
        return result.outputData() != null ? result.outputData() : Map.of("status", "SUCCESS");
    }

    private String snakeToCamel(String s) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (char c : s.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else if (nextUpper) {
                sb.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String camelToSnake(String s) {
        return s.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }

    private String sanitiseToolOutput(Map<String, Object> toolOutput) {
        String raw = safeJson(toolOutput);
        String cleaned = raw;
        for (Pattern p : INJECTION_PATTERNS) {
            cleaned = p.matcher(cleaned).replaceAll("[FILTERED]");
        }
        return "<tool_output_content>\n" + cleaned + "\n</tool_output_content>";
    }

    private String safeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }

    private static String trimTrailingSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}

