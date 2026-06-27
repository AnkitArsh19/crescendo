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
        return new App("gemini", "Google AI Studio (Gemini)", """
                Google Gemini is a powerful multimodal AI model. The Crescendo Gemini app lets you generate text, answer questions, summarize content, and analyze images using Google's generative AI.

                **What you can do with Gemini in Crescendo:**
                - Generate text responses for chatbots or automated replies
                - Summarize long emails, articles, or transcripts
                - Analyze and describe images using Gemini Vision models
                - Extract structured data from unstructured text

                **Actions available:**
                - Generate Text — pass a prompt and get an AI completion
                - Analyze Image — pass an image URL and a question to analyze visual content

                **Who should use this:** Teams wanting to add intelligent text or image processing to their workflows, such as auto-classifying support tickets or generating product descriptions.

                **Authentication:** API Key (create one at aistudio.google.com).
                """,
                "https://upload.wikimedia.org/wikipedia/commons/5/51/Gemini_sparkle_v002.svg", AuthType.APIKEY,
                List.of(),
                List.of(
                    Map.of(
                            "actionKey", "text-message",
                            "name", "Text - Message",
                            "description", "Generate text using a prompt",
                            "configSchema", List.of(
                                    Map.of("key", "model", "label", "Model", "type", "text", "required", true, "default", "gemini-1.5-pro"),
                                    Map.of("key", "prompt", "label", "Prompt", "type", "textarea", "required", true),
                                    Map.of("key", "temperature", "label", "Temperature", "type", "number", "required", false),
                                    Map.of("key", "maxOutputTokens", "label", "Max Tokens", "type", "number", "required", false)
                            )
                    ),
                    Map.of(
                            "actionKey", "image-analyze",
                            "name", "Image - Analyze",
                            "description", "Analyze an image using a prompt",
                            "configSchema", List.of(
                                    Map.of("key", "model", "label", "Model", "type", "text", "required", true, "default", "gemini-1.5-pro"),
                                    Map.of("key", "imageUrl", "label", "Image URL", "type", "text", "required", true),
                                    Map.of("key", "prompt", "label", "Prompt", "type", "textarea", "required", true)
                            )
                    )
                )
        ).credentialSchema(List.of(
            Map.of("key", "apiKey", "label", "API Key", "type", "password", "required", true)
        )).category("ai");
    }
}
