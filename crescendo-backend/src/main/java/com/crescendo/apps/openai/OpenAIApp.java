package com.crescendo.apps.openai;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class OpenAIApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("openai", "OpenAI", "Generate text with GPT models",
                "/icons/openai.svg", AuthType.APIKEY,
                List.of(),
                List.of(Map.of(
                    "actionKey", "chat-completion",
                    "name", "Chat Completion",
                    "description", "Generate a response using GPT",
                    "configSchema", Map.of(
                        "model", "string — model name (default: gpt-4o-mini)",
                        "prompt", "string (required) — the user prompt",
                        "systemPrompt", "string — optional system instructions",
                        "maxTokens", "integer — max response tokens (default: 1024)"
                    )
                ))
        )
        .credentialSchema(List.of(
            Map.of("key", "apiKey", "label", "API Key",
                    "type", "password", "required", true,
                    "placeholder", "sk-proj-...",
                    "helpText", "Create an API key at platform.openai.com/api-keys")
        ))
        .category("ai")
        .helpUrl("https://platform.openai.com/api-keys");
    }
}
