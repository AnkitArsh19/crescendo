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
                AuthType.APIKEY,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "agent:ai_agent",
                                "name", "AI Agent",
                                "description", "Autonomous agent that dynamically chooses tools and routes items during workflow execution.",
                                "configSchema", List.of(
                                        Map.of(
                                                "key", "provider",
                                                "label", "AI Provider",
                                                "type", "dropdown",
                                                "required", true,
                                                "default", "gemini",
                                                "options", List.of(
                                                        "gemini",
                                                        "openai",
                                                        "groq"
                                                ),
                                                "helpText", "Select AI provider: Google Gemini (default / platform key), OpenAI (BYOK), or Groq (BYOK)."
                                        ),
                                        Map.of(
                                                "key", "model",
                                                "label", "Model",
                                                "type", "dropdown",
                                                "required", false,
                                                "default", "gemma-4-26b",
                                                "options", List.of(
                                                        "gemma-4-26b",
                                                        "gemma-4-31b",
                                                        "gemini-3.5-flash-lite",
                                                        "gemini-3.8-flash",
                                                        "gpt-4o",
                                                        "gpt-4o-mini",
                                                        "llama-3.3-70b-versatile",
                                                        "llama-3.1-8b-instant"
                                                ),
                                                "helpText", "Recommended for agent loops: gemma-4-26b (14.4K RPD high throughput). For large context: gemini-3.5-flash-lite (500 RPD). Frontier: gemini-3.8-flash (20 RPD / BYOK)."
                                        ),
                                        Map.of(
                                                "key", "systemPrompt",
                                                "label", "System Prompt / Instructions",
                                                "type", "textarea",
                                                "required", true,
                                                "placeholder", "Instructions defining the agent's role, rules, and how it should use tools...",
                                                "default", "You are a helpful AI assistant. Analyze the incoming data and dynamically choose the appropriate tools to accomplish the goal.",
                                                "helpText", "Guides the agent's reasoning loop and tool selection behavior."
                                        ),
                                        Map.of(
                                                "key", "prompt",
                                                "label", "User Prompt / Input Data",
                                                "type", "textarea",
                                                "required", false,
                                                "placeholder", "Input data or instructions passed into the agent (e.g. {{steps.1.data}})...",
                                                "default", "{{steps.1.data}}",
                                                "helpText", "The main request or trigger data passed into the agent from previous steps."
                                        ),
                                        Map.of(
                                                "key", "temperature",
                                                "label", "Temperature",
                                                "type", "number",
                                                "required", false,
                                                "placeholder", "0.7",
                                                "default", 0.7,
                                                "helpText", "Controls randomness (0.0 = deterministic, 1.0 = creative)."
                                        ),
                                        Map.of(
                                                "key", "maxIterations",
                                                "label", "Max Reasoning Iterations",
                                                "type", "number",
                                                "required", false,
                                                "placeholder", "10",
                                                "default", 10,
                                                "helpText", "Maximum number of ReAct reasoning and tool-calling loops before stopping."
                                        ),
                                        Map.of(
                                                "key", "returnIntermediateSteps",
                                                "label", "Include Reasoning & Tool Observations",
                                                "type", "boolean",
                                                "required", false,
                                                "default", true,
                                                "helpText", "When enabled, output includes the agent's step-by-step reasoning and tool call logs."
                                        )
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "apiKey", "label", "API Key (optional if using platform Gemini)", "type", "password", "required", false)
        )).category("ai");
    }
}
