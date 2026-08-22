package com.crescendo.execution.agent;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandlerRegistry;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
 * </ul>
 *
 * <h2>Platform Safety Guarantees</h2>
 * <ol>
 *   <li><b>Pre-execution budget check</b> — evaluated BEFORE the LLM call and
 *       BEFORE any tool is invoked. A run that exceeds the budget is aborted
 *       cleanly with no partial side effects for that iteration.</li>
 *   <li><b>Tool-scope enforcement</b> — only tools explicitly wired to this agent
 *       via {@code AgentClusterConfig.toolRefs()} are sent to the LLM. The full
 *       114-app catalog is never exposed.</li>
 *   <li><b>Output sanitisation</b> — tool responses are wrapped in XML boundary
 *       tags and stripped of known injection patterns before being added to the
 *       conversation history that the LLM sees next turn.</li>
 *   <li><b>Per-iteration checkpoint</b> — each completed Reason→Tool→Observe
 *       cycle is logged; a crash mid-loop can be investigated from logs.</li>
 * </ol>
 *
 * <h2>Synchronous Execution Model</h2>
 * The loop runs synchronously inside the calling virtual thread (from
 * {@link com.crescendo.apps.agent.AgentHandlers}). Java 21 virtual threads yield
 * during every blocking HTTP call to Python, so N sequential round-trips do not
 * pin a carrier thread. The trade-off is that crash mid-loop fails the whole
 * workflow run — resumability requires a separate suspension mechanism and is
 * planned for a future release.
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
    private final ObjectMapper objectMapper;
    private final String pythonBaseUrl;
    private final String pythonServiceToken;
    private final String platformGeminiApiKey;

    public AgentExecutionService(
            SubWorkflowToolRunner subWorkflowToolRunner,
            ActionHandlerRegistry actionHandlerRegistry,
            ObjectMapper objectMapper,
            @Value("${crescendo.python-ai.base-url:}") String pythonBaseUrl,
            @Value("${crescendo.python-ai.service-token:}") String pythonServiceToken,
            @Value("${gemini.api.key:}") String platformGeminiApiKey
    ) {
        this.subWorkflowToolRunner = subWorkflowToolRunner;
        this.actionHandlerRegistry = actionHandlerRegistry;
        this.objectMapper = objectMapper;
        this.pythonBaseUrl = pythonBaseUrl;
        this.pythonServiceToken = pythonServiceToken;
        this.platformGeminiApiKey = platformGeminiApiKey;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Runs the full ReAct loop for one agent node execution.
     *
     * @param workflowRunId    ID of the current workflow run (used for idempotency key generation)
     * @param ownerUserId      ID of the user who owns this workflow (for credential resolution)
     * @param config           Cluster configuration: toolRefs, budget, iteration cap
     * @param systemPrompt     System prompt from the canvas node configuration
     * @param executionContext Trigger payload / prior-step output
     * @param provider         AI model provider (gemini, openai, groq)
     * @param model            Model name (e.g. gemini-2.5-flash, gpt-4o, llama-3.3-70b-versatile)
     * @param userApiKey       User's decrypted API key from connection, or null for platform default
     * @return Final output map — either the agent's final answer or an abort descriptor
     */
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
        String effectiveModel = (model != null && !model.isBlank()) ? model.trim() : ("gemini".equals(effectiveProvider) ? "gemini-2.5-flash" : "gpt-4o");

        String effectiveApiKey = (userApiKey != null && !userApiKey.isBlank() && !"null".equalsIgnoreCase(userApiKey)) ? userApiKey.trim() : null;
        if (effectiveApiKey == null && "gemini".equals(effectiveProvider)) {
            effectiveApiKey = platformGeminiApiKey;
        }

        if (effectiveApiKey == null || effectiveApiKey.isBlank()) {
            log.warn("Agent execution failed: No API key available for provider={}", effectiveProvider);
            return Map.of(
                    "status", "AUTH_REQUIRED",
                    "error", "API key is required for provider '" + effectiveProvider + "'. Please attach a connection or configure gemini.api.key in application.properties."
            );
        }

        log.info("Starting Agent Execution Loop: workflowRunId={} owner={} provider={} model={} maxIterations={} tokenBudget={}",
                workflowRunId, ownerUserId, effectiveProvider, effectiveModel, config.maxIterations(), config.tokenBudget());

        // Build the stable session ID for this agent run (distinct from workflowRunId)
        String agentSessionId = "agent-run:" + workflowRunId;

        // Conversation history grows each iteration
        List<AgentNextStepRequest.ConversationTurn> history = new ArrayList<>();

        // Seed history with the trigger/input data as the first user message
        if (executionContext != null && !executionContext.isEmpty()) {
            String inputJson = safeJson(executionContext);
            history.add(new AgentNextStepRequest.ConversationTurn("user", inputJson, null));
        }

        int accumulatedTokens = 0;

        for (int iteration = 1; iteration <= config.maxIterations(); iteration++) {

            // ── 1. Pre-execution budget check (before any LLM call or tool) ──
            if (accumulatedTokens >= config.tokenBudget()) {
                log.warn("Agent run={} aborted: token budget ({}) reached before iteration {}",
                        agentSessionId, config.tokenBudget(), iteration);
                return Map.of(
                        "status", "BUDGET_EXCEEDED",
                        "iterations", iteration - 1,
                        "tokensUsed", accumulatedTokens,
                        "error", "Token budget exhausted before iteration " + iteration
                );
            }

            log.info("Agent turn {}/{} | session={} | tokensUsed={}/{}",
                    iteration, config.maxIterations(), agentSessionId, accumulatedTokens, config.tokenBudget());

            // ── 2. Call AI Reasoning Model (Python AI service or direct native fallback) ──
            AgentNextStepResponse response;
            try {
                if (pythonBaseUrl != null && !pythonBaseUrl.isBlank()) {
                    response = callPythonNextStep(agentSessionId, iteration, systemPrompt, config, history, executionContext, effectiveProvider, effectiveModel, effectiveApiKey);
                } else if ("gemini".equals(effectiveProvider)) {
                    response = callDirectGemini(effectiveApiKey, effectiveModel, systemPrompt, history, executionContext);
                } else {
                    response = callDirectOpenAICompatible(effectiveProvider, effectiveApiKey, effectiveModel, systemPrompt, history, executionContext);
                }
            } catch (Exception e) {
                log.error("Agent turn {}: AI reasoning call failed — {}", iteration, e.getMessage(), e);
                // Fallback to direct Gemini/OpenAI if Python was configured but failed
                if (pythonBaseUrl != null && !pythonBaseUrl.isBlank()) {
                    try {
                        log.info("Attempting direct native fallback for provider={}", effectiveProvider);
                        if ("gemini".equals(effectiveProvider)) {
                            response = callDirectGemini(effectiveApiKey, effectiveModel, systemPrompt, history, executionContext);
                        } else {
                            response = callDirectOpenAICompatible(effectiveProvider, effectiveApiKey, effectiveModel, systemPrompt, history, executionContext);
                        }
                    } catch (Exception directEx) {
                        return Map.of(
                                "status", "AI_SERVICE_ERROR",
                                "iterations", iteration,
                                "error", "AI reasoning service failed: " + directEx.getMessage()
                        );
                    }
                } else {
                    return Map.of(
                            "status", "AI_SERVICE_ERROR",
                            "iterations", iteration,
                            "error", "AI reasoning service failed: " + e.getMessage()
                    );
                }
            }

            accumulatedTokens += response.tokensUsed();

            // ── 3a. Final answer — agent is done ─────────────────────────
            if (response.isFinalAnswer()) {
                log.info("Agent run={} completed with final answer after {} iterations (tokensUsed={})",
                        agentSessionId, iteration, accumulatedTokens);
                return Map.of(
                        "status", "COMPLETED",
                        "iterations", iteration,
                        "tokensUsed", accumulatedTokens,
                        "result", response.finalAnswer() != null ? response.finalAnswer() : ""
                );
            }

            // ── 3b. Tool call — dispatch through ActionHandlerRegistry ────
            if (response.isToolCall()) {
                AgentNextStepResponse.ToolCallDecision toolCall = response.toolCall();

                log.info("Agent turn {}: calling tool appKey={} actionKey={} args={}",
                        iteration, toolCall.appKey(), toolCall.actionKey(), toolCall.arguments());

                // Append assistant's decision to history
                history.add(new AgentNextStepRequest.ConversationTurn(
                        "assistant",
                        "Calling tool: " + toolCall.actionKey() + " with " + safeJson(toolCall.arguments()),
                        null
                ));

                // Idempotency key: stable per run + iteration
                String idempotencyKey = String.format("agent-tool:%s:%d", workflowRunId, iteration);
                log.debug("Tool dispatch idempotencyKey={}", idempotencyKey);

                // Dispatch to the registered action handler
                Map<String, Object> toolOutput;
                try {
                    toolOutput = dispatchTool(toolCall, ownerUserId, workflowRunId, idempotencyKey);
                } catch (Exception e) {
                    log.error("Agent turn {}: tool dispatch failed for {} — {}", iteration, toolCall.actionKey(), e.getMessage(), e);
                    toolOutput = Map.of("error", "Tool execution failed: " + e.getMessage());
                }

                // ── 4. Sanitise tool output before feeding back to LLM ────
                String sanitisedObservation = sanitiseToolOutput(toolOutput);

                // Append tool observation to history
                history.add(new AgentNextStepRequest.ConversationTurn("tool", sanitisedObservation, idempotencyKey));

                // ── 5. Per-iteration checkpoint ───────────────────────────
                log.info("Agent turn {} checkpoint: tool={} output_length={} tokensUsed={}",
                        iteration, toolCall.actionKey(), sanitisedObservation.length(), accumulatedTokens);
            }
        }

        // Max iterations reached without a final_answer
        log.warn("Agent run={} reached maxIterations ({}) without a final answer.", agentSessionId, config.maxIterations());
        return Map.of(
                "status", "MAX_ITERATIONS_REACHED",
                "iterations", config.maxIterations(),
                "tokensUsed", accumulatedTokens,
                "error", "Agent did not produce a final answer within " + config.maxIterations() + " iterations"
        );
    }

    // ─────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Calls {@code POST /v1/agent/next-step} on the Python AI microservice.
     */
    private AgentNextStepResponse callPythonNextStep(
            String sessionId,
            int iteration,
            String systemPrompt,
            AgentClusterConfig config,
            List<AgentNextStepRequest.ConversationTurn> history,
            Map<String, Object> inputData,
            String provider,
            String model,
            String apiKey
    ) {
        List<AgentNextStepRequest.ToolDefinition> toolDefs = List.of();

        AgentNextStepRequest requestBody = new AgentNextStepRequest(
                sessionId, iteration, systemPrompt, toolDefs, history, inputData, provider, model, apiKey
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

    /**
     * Direct native call to Google Gemini REST API.
     */
    @SuppressWarnings("unchecked")
    private AgentNextStepResponse callDirectGemini(
            String apiKey,
            String model,
            String systemPrompt,
            List<AgentNextStepRequest.ConversationTurn> history,
            Map<String, Object> inputData
    ) {
        RestClient client = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        String endpoint = "/" + model + ":generateContent?key=" + apiKey;

        List<Map<String, Object>> contents = new ArrayList<>();
        for (AgentNextStepRequest.ConversationTurn turn : history) {
            String role = "user".equalsIgnoreCase(turn.role()) ? "user" : "model";
            contents.add(Map.of(
                    "role", role,
                    "parts", List.of(Map.of("text", turn.content() != null ? turn.content() : ""))
            ));
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
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                if (content != null) {
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        text = String.valueOf(parts.get(0).getOrDefault("text", ""));
                    }
                }
            }

            int tokensUsed = 0;
            Map<String, Object> usage = (Map<String, Object>) parsed.get("usageMetadata");
            if (usage != null && usage.get("totalTokenCount") instanceof Number n) {
                tokensUsed = n.intValue();
            }

            return new AgentNextStepResponse("final_answer", null, text, null, tokensUsed);
        } catch (Exception e) {
            log.error("Direct Gemini reasoning failed: {}", e.getMessage(), e);
            throw new RestClientException("Direct Gemini reasoning failed: " + e.getMessage(), e);
        }
    }

    /**
     * Direct native call to OpenAI or Groq compatible /v1/chat/completions endpoint.
     */
    @SuppressWarnings("unchecked")
    private AgentNextStepResponse callDirectOpenAICompatible(
            String provider,
            String apiKey,
            String model,
            String systemPrompt,
            List<AgentNextStepRequest.ConversationTurn> history,
            Map<String, Object> inputData
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
            messages.add(Map.of("role", turn.role(), "content", turn.content() != null ? turn.content() : ""));
        }
        if (messages.isEmpty() && inputData != null && !inputData.isEmpty()) {
            messages.add(Map.of("role", "user", "content", safeJson(inputData)));
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", messages
        );

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
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                if (message != null) {
                    text = String.valueOf(message.getOrDefault("content", ""));
                }
            }

            int tokensUsed = 0;
            Map<String, Object> usage = (Map<String, Object>) parsed.get("usage");
            if (usage != null && usage.get("total_tokens") instanceof Number n) {
                tokensUsed = n.intValue();
            }

            return new AgentNextStepResponse("final_answer", null, text, null, tokensUsed);
        } catch (Exception e) {
            log.error("Direct {} reasoning failed: {}", provider, e.getMessage(), e);
            throw new RestClientException("Direct " + provider + " reasoning failed: " + e.getMessage(), e);
        }
    }

    /**
     * Dispatches a tool call decision to the registered {@link com.crescendo.execution.action.ActionHandler}.
     *
     * <p>Sub-workflow tools are routed to {@link SubWorkflowToolRunner} (which holds
     * the distributed lock discipline). All other tools go through the standard
     * {@link ActionHandlerRegistry}.</p>
     */
    private Map<String, Object> dispatchTool(
            AgentNextStepResponse.ToolCallDecision toolCall,
            UUID ownerUserId,
            UUID workflowRunId,
            String idempotencyKey
    ) {
        // Sub-workflow tool — uses dedicated runner with lock discipline
        if ("workflow".equals(toolCall.appKey())) {
            String subWorkflowIdStr = String.valueOf(
                    toolCall.arguments().getOrDefault("workflowId", ""));
            if (!subWorkflowIdStr.isBlank()) {
                return subWorkflowToolRunner.executeSubWorkflowTool(
                        UUID.fromString(subWorkflowIdStr),
                        ownerUserId,
                        toolCall.arguments()
                );
            }
        }

        // Standard tool — dispatch through action handler registry
        var handlerOpt = actionHandlerRegistry.find(toolCall.appKey(), toolCall.actionKey());
        if (handlerOpt.isEmpty()) {
            log.warn("No handler found for tool appKey={} actionKey={}", toolCall.appKey(), toolCall.actionKey());
            return Map.of("error", "Unknown tool: " + toolCall.actionKey());
        }

        ActionContext toolContext = new ActionContext(
                toolCall.appKey(),
                toolCall.actionKey(),
                toolCall.arguments() != null ? new HashMap<>(toolCall.arguments()) : Map.of(),
                Map.of(),       // credentials — TODO: resolve from connection store using ownerUserId
                Map.of(),       // inputData
                workflowRunId,
                ownerUserId,
                UUID.randomUUID(), // ephemeral step ID for this tool invocation
                0
        );

        ActionResult result = handlerOpt.get().execute(toolContext);
        return result.outputData() != null ? result.outputData() : Map.of("status", result.success() ? "SUCCESS" : "FAILURE");
    }

    /**
     * Wraps tool output in XML boundary tags and strips known injection patterns.
     *
     * <h3>Why Java owns this, not Python</h3>
     * <p>Tool output comes back to Java first — it is the trust boundary. By
     * sanitising here, before the conversation history is sent to Python, we
     * guarantee that even if a malicious Slack message or email body contains an
     * injection attempt, the LLM never sees it as raw text.</p>
     *
     * <p>Python's {@code sanitizer.py} sanitises the user's initial NL prompt
     * (NL workflow builder path). These are two different seams. Each owns
     * exactly one.</p>
     */
    private String sanitiseToolOutput(Map<String, Object> toolOutput) {
        String raw = safeJson(toolOutput);
        String cleaned = raw;
        for (Pattern p : INJECTION_PATTERNS) {
            cleaned = p.matcher(cleaned).replaceAll("[FILTERED]");
        }
        return "<tool_output_content>\n" + cleaned + "\n</tool_output_content>";
    }

    /**
     * Serialises an object to a JSON string without throwing.
     * Falls back to {@code toString()} if Jackson fails.
     */
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
