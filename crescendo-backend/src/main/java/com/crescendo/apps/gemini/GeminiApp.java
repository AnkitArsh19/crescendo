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
        return new App("gemini", "Google AI Studio (Gemini)", "Generate text, analyze images with Google Gemini",
                "https://upload.wikimedia.org/wikipedia/commons/5/51/Gemini_sparkle_v002.svg", AuthType.APIKEY,
                List.of(),
                List.of(
                    Map.of("actionKey", "generate-text", "name", "Generate Text",
                        "description", "Generate text with Gemini",
                        "configSchema", List.of(
                            Map.of("key", "prompt", "label", "Prompt", "type", "textarea", "required", true,
                                   "placeholder", "Explain quantum computing...", "helpText", "The user prompt"),
                            Map.of("key", "model", "label", "Model", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "gemini-2.0-flash", "label", "Gemini 2.0 Flash (fast)"),
                                       Map.of("value", "gemini-1.5-pro", "label", "Gemini 1.5 Pro"),
                                       Map.of("value", "gemini-1.5-flash", "label", "Gemini 1.5 Flash")
                                   ), "helpText", "Model to use"),
                            Map.of("key", "temperature", "label", "Temperature", "type", "text", "required", false,
                                   "placeholder", "0.7", "helpText", "0 = deterministic, 1 = creative"),
                            Map.of("key", "maxOutputTokens", "label", "Max Tokens", "type", "text", "required", false,
                                   "placeholder", "2048", "helpText", "Max response length"))),
                    Map.of("actionKey", "analyze-image", "name", "Analyze Image",
                        "description", "Analyze an image with Gemini vision",
                        "configSchema", List.of(
                            Map.of("key", "imageUrl", "label", "Image URL", "type", "text", "required", true,
                                   "placeholder", "https://upload.wikimedia.org/wikipedia/commons/5/51/Gemini_sparkle_v002.svg", "helpText", "URL of the image"),
                            Map.of("key", "prompt", "label", "Question", "type", "textarea", "required", true,
                                   "placeholder", "What is in this image?", "helpText", "What to analyze"),
                            Map.of("key", "model", "label", "Model", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "gemini-2.0-flash", "label", "Gemini 2.0 Flash"),
                                       Map.of("value", "gemini-1.5-pro", "label", "Gemini 1.5 Pro")
                                   ), "helpText", "Vision model")))
                )
        ).credentialSchema(List.of(
            Map.of("key", "apiKey", "label", "API Key", "type", "password", "required", true,
                    "placeholder", "AIzaSy...", "helpText", "Create at aistudio.google.com/app/apikey")
        )).category("ai").helpUrl("https://upload.wikimedia.org/wikipedia/commons/5/51/Gemini_sparkle_v002.svg");
    }
}
