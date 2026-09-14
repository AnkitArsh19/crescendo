package com.crescendo.apps.agent;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import com.crescendo.execution.agent.AgentClusterConfig;
import com.crescendo.execution.agent.AgentExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Action mapping handler for AI Agent Cluster Nodes (agent:ai_agent).
 * Delegates execution directly to AgentExecutionService during workflow runtime.
 */
@Component
public class AgentHandlers {

    private static final Logger log = LoggerFactory.getLogger(AgentHandlers.class);

    private final AgentExecutionService agentExecutionService;

    public AgentHandlers(@Lazy AgentExecutionService agentExecutionService) {
        this.agentExecutionService = agentExecutionService;
    }

    @ActionMapping(appKey = "agent", actionKey = "agent:ai_agent")
    public ActionResult executeAgent(ActionContext ctx) {
        return handleExecution(ctx);
    }

    @ActionMapping(appKey = "agent", actionKey = "ai_agent")
    public ActionResult executeAgentAlias(ActionContext ctx) {
        return handleExecution(ctx);
    }

    private ActionResult handleExecution(ActionContext ctx) {
        log.info("Executing Agent Node: stepId={} workflowRunId={}", ctx.stepId(), ctx.workflowRunId());

        Map<String, Object> config = ctx.configuration() != null ? ctx.configuration() : Map.of();
        Map<String, Object> creds = ctx.credentials() != null ? ctx.credentials() : Map.of();

        Object sysPromptObj = config.get("systemPrompt");
        if (sysPromptObj == null || sysPromptObj.toString().isBlank()) {
            sysPromptObj = config.get("goal");
        }
        if (sysPromptObj == null || sysPromptObj.toString().isBlank()) {
            sysPromptObj = config.get("instructions");
        }
        String systemPrompt = (sysPromptObj != null && !sysPromptObj.toString().isBlank())
                ? sysPromptObj.toString()
                : "You are a helpful AI assistant. Analyze the incoming data and dynamically choose the appropriate tools to accomplish the goal.";

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

        java.util.Map<String, Object> effectiveInput = new java.util.HashMap<>();
        if (ctx.inputData() != null) {
            effectiveInput.putAll(ctx.inputData());
        }
        Object promptObj = config.get("prompt");
        if (promptObj != null && !promptObj.toString().isBlank() && !effectiveInput.containsKey("prompt")) {
            effectiveInput.put("prompt", promptObj.toString());
        }
        if (effectiveInput.isEmpty()) {
            effectiveInput.put("input", "Proceed with your instructions.");
        }

        List<UUID> toolRefs = new java.util.ArrayList<>();
        List<AgentClusterConfig.ToolConfig> configuredTools = new java.util.ArrayList<>();

        Object agentConfigObj = config.get("agentConfig");
        if (agentConfigObj instanceof Map<?, ?> acMap) {
            Object trObj = acMap.get("toolRefs");
            if (trObj instanceof List<?> trList) {
                for (Object item : trList) {
                    try {
                        if (item != null) toolRefs.add(UUID.fromString(item.toString().trim()));
                    } catch (Exception ignored) {}
                }
            }
            Object ctObj = acMap.get("configuredTools");
            if (ctObj instanceof List<?> ctList) {
                for (Object item : ctList) {
                    if (item instanceof Map<?, ?> itemMap) {
                        configuredTools.add(parseToolConfig(itemMap));
                    }
                }
            }
        }

        // Direct "tools" list in configuration (from canvas config panel)
        Object toolsObj = config.get("tools");
        if (toolsObj instanceof List<?> toolsList) {
            for (Object item : toolsList) {
                if (item instanceof Map<?, ?> itemMap) {
                    configuredTools.add(parseToolConfig(itemMap));
                } else if (item instanceof String s && s.contains(":")) {
                    String[] parts = s.split(":", 2);
                    configuredTools.add(new AgentClusterConfig.ToolConfig(parts[0].trim(), parts[1].trim()));
                }
            }
        }

        AgentClusterConfig agentClusterConfig = new AgentClusterConfig(
                toolRefs,
                configuredTools,
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
                effectiveInput,
                provider,
                model,
                apiKey
        );

        if ("AUTH_REQUIRED".equals(result.get("status")) || "AI_SERVICE_ERROR".equals(result.get("status"))) {
            return ActionResult.failure(String.valueOf(result.getOrDefault("error", "AI execution failed")));
        }

        return ActionResult.success(result);
    }

    @SuppressWarnings("unchecked")
    private AgentClusterConfig.ToolConfig parseToolConfig(Map<?, ?> m) {
        String appKey = m.get("appKey") != null ? m.get("appKey").toString() : "";
        String actionKey = m.get("actionKey") != null ? m.get("actionKey").toString() : "";
        String toolId = m.get("toolId") != null ? m.get("toolId").toString() : null;
        String customName = m.get("customName") != null ? m.get("customName").toString() : null;
        String customDescription = m.get("customDescription") != null ? m.get("customDescription").toString() : null;

        UUID connectionId = null;
        if (m.get("connectionId") != null && !m.get("connectionId").toString().isBlank()) {
            try {
                connectionId = UUID.fromString(m.get("connectionId").toString().trim());
            } catch (Exception ignored) {}
        }

        UUID subWorkflowId = null;
        if (m.get("subWorkflowId") != null && !m.get("subWorkflowId").toString().isBlank()) {
            try {
                subWorkflowId = UUID.fromString(m.get("subWorkflowId").toString().trim());
            } catch (Exception ignored) {}
        }

        Map<String, Object> fixedParams = m.get("fixedParameters") instanceof Map<?, ?> fp
                ? (Map<String, Object>) fp
                : Map.of();

        return new AgentClusterConfig.ToolConfig(
                toolId,
                appKey,
                actionKey,
                customName,
                customDescription,
                connectionId,
                subWorkflowId,
                fixedParams
        );
    }
}
