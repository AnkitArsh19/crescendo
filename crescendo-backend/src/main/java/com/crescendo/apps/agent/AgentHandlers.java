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
import java.util.UUID;

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
        String systemPrompt = String.valueOf(config.getOrDefault("systemPrompt", "Analyze and route input."));

        AgentClusterConfig agentClusterConfig = new AgentClusterConfig(
                List.of(),
                null,
                null,
                null,
                10,
                3,
                50000
        );

        Map<String, Object> result = agentExecutionService.executeAgentLoop(
                ctx.workflowRunId(),
                ctx.userId(),
                agentClusterConfig,
                systemPrompt,
                ctx.inputData()
        );

        return ActionResult.success(result);
    }
}
