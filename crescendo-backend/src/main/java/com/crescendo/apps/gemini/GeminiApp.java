package com.crescendo.apps.gemini;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GeminiApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("gemini", "Google AI Studio (Gemini)", "Generate text with Google Gemini models",
                "/icons/gemini.svg", AuthType.APIKEY,
                List.of(),
                List.of(Map.of(
                    "actionKey", "generate-text",
                    "name", "Generate Text",
                    "description", "Generate text using Gemini",
                    "configSchema", Map.of(
                        "prompt", "string (required) — the user prompt",
                        "model", "string — model name (default: gemini-2.0-flash)"
                    )
                ))
        )
        .credentialSchema(List.of(
            Map.of("key", "apiKey", "label", "API Key",
                    "type", "password", "required", true,
                    "placeholder", "AIzaSy...",
                    "helpText", "Create an API key at aistudio.google.com/app/apikey. Free tier available.")
        ))
        .category("ai")
        .helpUrl("https://aistudio.google.com/app/apikey");
    }
}
