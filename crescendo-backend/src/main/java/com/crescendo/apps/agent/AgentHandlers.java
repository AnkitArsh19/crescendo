package com.crescendo.apps.agent;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import com.crescendo.execution.agent.AgentClusterConfig;
import com.crescendo.execution.agent.AgentExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Action mapping handler for AI Agent Cluster Nodes (agent:ai_agent).
 * Delegates execution directly to AgentExecutionService during workflow runtime.
 */
@Component
public class AgentHandlers {

    private static final Logger log = LoggerFactory.getLogger(AgentHandlers.class);

    private final AgentExecutionService agentExecutionService;

    public AgentHandlers(AgentExecutionService agentExecutionService) {
        this.agentExecutionService = agentExecutionService;
    }

    @ActionMapping(appKey = "agent", actionKey = "agent:ai_agent")
    public ActionResult executeAgent(ActionContext ctx) {
        log.info("Executing Agent Node: stepId={} workflowRunId={}", ctx.stepId(), ctx.workflowRunId());

        Map<String, Object> config = ctx.configuration() != null ? ctx.configuration() : Map.of();
        Map<String, Object> creds = ctx.credentials() != null ? ctx.credentials() : Map.of();

        String systemPrompt = String.valueOf(config.getOrDefault(
                "systemPrompt",
                "You are a helpful AI assistant. Analyze the incoming data and dynamically choose the appropriate tools to accomplish the goal."
        ));

        String provider = String.valueOf(config.getOrDefault("provider", "gemini"));
        String model = String.valueOf(config.getOrDefault("model", "gemini-3.5-flash-lite"));
        String apiKey = creds.get("apiKey") != null ? String.valueOf(creds.get("apiKey")) : null;

        int maxIterations = 10;
        if (config.get("maxIterations") != null) {
            try {
                maxIterations = Integer.parseInt(String.valueOf(config.get("maxIterations")));
            } catch (NumberFormatException ignored) {}
        }

        int tokenBudget = 50000;
        if (config.get("tokenBudget") != null) {
            try {
                tokenBudget = Integer.parseInt(String.valueOf(config.get("tokenBudget")));
            } catch (NumberFormatException ignored) {}
        }

        AgentClusterConfig agentClusterConfig = new AgentClusterConfig(
                List.of(),
                null,
                null,
                null,
                maxIterations,
                3,
                tokenBudget
        );

        Map<String, Object> result = agentExecutionService.executeAgentLoop(
                ctx.workflowRunId(),
                ctx.userId(),
                agentClusterConfig,
                systemPrompt,
                ctx.inputData(),
                provider,
                model,
                apiKey
        );

        if ("AUTH_REQUIRED".equals(result.get("status")) || "AI_SERVICE_ERROR".equals(result.get("status"))) {
            return ActionResult.failure(String.valueOf(result.getOrDefault("error", "AI execution failed")));
        }

        return ActionResult.success(result);
    }
}
