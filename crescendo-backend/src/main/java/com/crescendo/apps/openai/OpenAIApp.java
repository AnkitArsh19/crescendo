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
        return new App("openai", "OpenAI", "Generate text, images, and embeddings with GPT and DALL-E",
                "/icons/openai.svg", AuthType.APIKEY,
                List.of(),
                List.of(
                    Map.of("actionKey", "chat-completion", "name", "Chat Completion",
                        "description", "Generate a response using GPT",
                        "configSchema", List.of(
                            Map.of("key", "prompt", "label", "Prompt", "type", "textarea", "required", true,
                                   "placeholder", "Summarize this article...", "helpText", "The user prompt"),
                            Map.of("key", "model", "label", "Model", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "gpt-4o-mini", "label", "GPT-4o Mini (fast)"),
                                       Map.of("value", "gpt-4o", "label", "GPT-4o"),
                                       Map.of("value", "gpt-4-turbo", "label", "GPT-4 Turbo"),
                                       Map.of("value", "gpt-3.5-turbo", "label", "GPT-3.5 Turbo")
                                   ), "helpText", "Model to use"),
                            Map.of("key", "systemPrompt", "label", "System Prompt", "type", "textarea", "required", false,
                                   "placeholder", "You are a helpful assistant.", "helpText", "System role instructions"),
                            Map.of("key", "maxTokens", "label", "Max Tokens", "type", "text", "required", false,
                                   "placeholder", "1024", "helpText", "Maximum response length"),
                            Map.of("key", "temperature", "label", "Temperature", "type", "text", "required", false,
                                   "placeholder", "0.7", "helpText", "0 = deterministic, 1 = creative"))),
                    Map.of("actionKey", "generate-image", "name", "Generate Image (DALL-E)",
                        "description", "Create an image from a text description",
                        "configSchema", List.of(
                            Map.of("key", "prompt", "label", "Image Description", "type", "textarea", "required", true,
                                   "placeholder", "A futuristic city at sunset", "helpText", "Describe the image you want"),
                            Map.of("key", "model", "label", "Model", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "dall-e-3", "label", "DALL-E 3"),
                                       Map.of("value", "dall-e-2", "label", "DALL-E 2")
                                   ), "helpText", "Image generation model"),
                            Map.of("key", "size", "label", "Size", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "1024x1024", "label", "1024×1024 (Square)"),
                                       Map.of("value", "1792x1024", "label", "1792×1024 (Landscape)"),
                                       Map.of("value", "1024x1792", "label", "1024×1792 (Portrait)")
                                   ), "helpText", "Image dimensions"),
                            Map.of("key", "quality", "label", "Quality", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "standard", "label", "Standard"),
                                       Map.of("value", "hd", "label", "HD")
                                   ), "helpText", "Image quality"))),
                    Map.of("actionKey", "generate-embeddings", "name", "Generate Embeddings",
                        "description", "Convert text to a numerical vector for similarity search",
                        "configSchema", List.of(
                            Map.of("key", "input", "label", "Text", "type", "textarea", "required", true,
                                   "helpText", "Text to embed"),
                            Map.of("key", "model", "label", "Model", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "text-embedding-3-small", "label", "Embedding 3 Small"),
                                       Map.of("value", "text-embedding-3-large", "label", "Embedding 3 Large")
                                   ), "helpText", "Embedding model")))
                )
        ).credentialSchema(List.of(
            Map.of("key", "apiKey", "label", "API Key", "type", "password", "required", true,
                    "placeholder", "sk-proj-...", "helpText", "Create at platform.openai.com/api-keys")
        )).category("ai").helpUrl("https://platform.openai.com/api-keys");
    }
}
