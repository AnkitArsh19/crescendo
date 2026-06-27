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
        return new App("openai", "OpenAI", """
                OpenAI provides industry-leading AI models including GPT-4 and DALL-E. The Crescendo OpenAI app lets you integrate advanced natural language processing and image generation into your workflows.

                **What you can do with OpenAI in Crescendo:**
                - Generate human-like text responses and chat completions
                - Create images from text descriptions using DALL-E
                - Convert text into numerical vector embeddings for semantic search
                - Automate content creation, translation, and data extraction

                **Actions available:**
                - Chat Completion — prompt GPT models for intelligent text generation
                - Generate Image — create visuals based on descriptive prompts
                - Generate Embeddings — turn text into vectors for AI applications

                **Who should use this:** Marketers for content generation, engineers building AI-powered search, and support teams for triaging customer feedback.

                **Authentication:** API Key (create one at platform.openai.com).
                """,
                "https://www.google.com/s2/favicons?domain=openai.com&sz=128", AuthType.APIKEY,
                List.of(),
                List.of(
                    Map.of(
                            "actionKey", "chat-complete",
                            "name", "Chat - Complete",
                            "description", "Create one or more completions for a given text",
                            "configSchema", List.of(
                                    Map.of("key", "model", "label", "Model", "type", "text", "required", true, "default", "gpt-3.5-turbo"),
                                    Map.of("key", "messages", "label", "Messages", "type", "json", "required", true),
                                    Map.of("key", "simplifyOutput", "label", "Simplify", "type", "boolean", "required", false, "default", true),
                                    Map.of("key", "echo", "label", "Echo Prompt", "type", "boolean", "required", false),
                                    Map.of("key", "frequency_penalty", "label", "Frequency Penalty", "type", "number", "required", false),
                                    Map.of("key", "max_tokens", "label", "Max Tokens", "type", "number", "required", false),
                                    Map.of("key", "n", "label", "Number of Completions", "type", "number", "required", false),
                                    Map.of("key", "presence_penalty", "label", "Presence Penalty", "type", "number", "required", false),
                                    Map.of("key", "temperature", "label", "Temperature", "type", "number", "required", false),
                                    Map.of("key", "top_p", "label", "Top P", "type", "number", "required", false)
                            )
                    ),
                    Map.of(
                            "actionKey", "image-create",
                            "name", "Image - Create",
                            "description", "Create an image for a given text",
                            "configSchema", List.of(
                                    Map.of("key", "prompt", "label", "Prompt", "type", "text", "required", true),
                                    Map.of("key", "model", "label", "Model", "type", "text", "required", false, "default", "dall-e-2"),
                                    Map.of("key", "responseFormat", "label", "Response Format", "type", "text", "required", false, "default", "url"),
                                    Map.of("key", "n", "label", "Number of Images", "type", "number", "required", false),
                                    Map.of("key", "quality", "label", "Quality", "type", "text", "required", false),
                                    Map.of("key", "size", "label", "Size", "type", "text", "required", false),
                                    Map.of("key", "style", "label", "Style", "type", "text", "required", false)
                            )
                    ),
                    Map.of(
                            "actionKey", "text-complete",
                            "name", "Text - Complete",
                            "description", "Create one or more completions for a given text",
                            "configSchema", List.of(
                                    Map.of("key", "model", "label", "Model", "type", "text", "required", true, "default", "gpt-3.5-turbo-instruct"),
                                    Map.of("key", "prompt", "label", "Prompt", "type", "text", "required", true),
                                    Map.of("key", "simplifyOutput", "label", "Simplify", "type", "boolean", "required", false, "default", true),
                                    Map.of("key", "echo", "label", "Echo Prompt", "type", "boolean", "required", false),
                                    Map.of("key", "frequency_penalty", "label", "Frequency Penalty", "type", "number", "required", false),
                                    Map.of("key", "max_tokens", "label", "Max Tokens", "type", "number", "required", false),
                                    Map.of("key", "n", "label", "Number of Completions", "type", "number", "required", false),
                                    Map.of("key", "presence_penalty", "label", "Presence Penalty", "type", "number", "required", false),
                                    Map.of("key", "temperature", "label", "Temperature", "type", "number", "required", false),
                                    Map.of("key", "top_p", "label", "Top P", "type", "number", "required", false)
                            )
                    ),
                    Map.of(
                            "actionKey", "text-edit",
                            "name", "Text - Edit",
                            "description", "Edit or rewrite text based on an instruction (uses GPT chat API)",
                            "configSchema", List.of(
                                    Map.of("key", "model", "label", "Model", "type", "text", "required", false, "default", "gpt-4o-mini"),
                                    Map.of("key", "input", "label", "Input Text", "type", "textarea", "required", true),
                                    Map.of("key", "instruction", "label", "Instruction", "type", "text", "required", true),
                                    Map.of("key", "simplifyOutput", "label", "Simplify Output", "type", "boolean", "required", false, "default", true)
                            )
                    ),
                    Map.of(
                            "actionKey", "text-moderate",
                            "name", "Text - Moderate",
                            "description", "Classify if a text violates OpenAI's content policy (uses /v1/moderations)",
                            "configSchema", List.of(
                                    Map.of("key", "model", "label", "Model", "type", "text", "required", false, "default", "omni-moderation-latest"),
                                    Map.of("key", "input", "label", "Input", "type", "textarea", "required", true),
                                    Map.of("key", "simplifyOutput", "label", "Simplify Output", "type", "boolean", "required", false, "default", true)
                            )
                    )
                )
        ).credentialSchema(List.of(
            Map.of("key", "apiKey", "label", "API Key", "type", "password", "required", true)
        )).category("ai");
    }
}
