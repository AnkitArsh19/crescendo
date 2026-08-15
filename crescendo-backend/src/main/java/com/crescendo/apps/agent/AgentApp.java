package com.crescendo.apps.agent;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for AI Agent Cluster Node.
 * Registers the 'agent' app and 'agent:ai_agent' action in the App Catalog.
 */
@Component
public class AgentApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "agent",
                "AI Agent",
                """
                Autonomous Agentic AI node for dynamic runtime decision making and tool orchestration.
                
                Attaches tools, language models, memory, and output parsers as sub-nodes on the canvas.
                Operates during active workflow execution to classify, route, and execute actions dynamically.
                """,
                "/icons/agent.svg",
                AuthType.NONE,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "agent:ai_agent",
                                "name", "AI Agent",
                                "description", "Autonomous agent that dynamically chooses tools and routes items during workflow execution.",
                                "configSchema", List.of(
                                        Map.of("key", "systemPrompt", "label", "System Prompt / Instructions", "type", "string", "required", true, "default", "You are a helpful AI assistant. Analyze the incoming data and pick the right tool to complete the task."),
                                        Map.of("key", "agentConfig", "label", "Cluster Configuration", "type", "json")
                                )
                        )
                )
        ).credentialSchema(List.of()).category("ai");
    }
}
