package com.crescendo.apps.dalle;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DallEApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("dall-e", "DALL·E", "Generate images from text prompts using OpenAI's DALL·E",
                "/icons/dall-e.svg", AuthType.APIKEY,
                List.of(),
                List.of(Map.of(
                    "actionKey", "generate-image",
                    "name", "Generate Image",
                    "description", "Create an image from a text prompt",
                    "configSchema", Map.of(
                        "prompt", "string (required) — image description",
                        "size", "string — image size (default: 1024x1024)",
                        "model", "string — model name (default: dall-e-3)"
                    )
                ))
        )
        .credentialSchema(List.of(
            Map.of("key", "apiKey", "label", "OpenAI API Key",
                    "type", "password", "required", true,
                    "placeholder", "sk-proj-...",
                    "helpText", "Uses the same API key as OpenAI. Get it at platform.openai.com/api-keys")
        ))
        .category("ai")
        .helpUrl("https://platform.openai.com/api-keys");
    }
}
