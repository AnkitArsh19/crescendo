package com.crescendo.apps.sarvam;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class SarvamApp implements AppDefinition {
    @Override
    public App toApp() {
        var langOptions = List.of(
            Map.of("value", "hi-IN", "label", "Hindi"),
            Map.of("value", "ta-IN", "label", "Tamil"),
            Map.of("value", "te-IN", "label", "Telugu"),
            Map.of("value", "kn-IN", "label", "Kannada"),
            Map.of("value", "ml-IN", "label", "Malayalam"),
            Map.of("value", "mr-IN", "label", "Marathi"),
            Map.of("value", "bn-IN", "label", "Bengali"),
            Map.of("value", "gu-IN", "label", "Gujarati"),
            Map.of("value", "pa-IN", "label", "Punjabi"),
            Map.of("value", "en-IN", "label", "English (India)")
        );

        return new App("sarvam", "Sarvam AI", "Indian language AI — translation, TTS, and speech-to-text",
                "/icons/sarvam.svg", AuthType.APIKEY,
                List.of(),
                List.of(
                    Map.of("actionKey", "translate", "name", "Translate Text",
                        "description", "Translate text between Indian languages",
                        "configSchema", List.of(
                            Map.of("key", "text", "label", "Text", "type", "textarea", "required", true,
                                   "placeholder", "Translate this text...", "helpText", "Content to translate"),
                            Map.of("key", "sourceLang", "label", "Source Language", "type", "select", "required", true,
                                   "options", langOptions, "helpText", "Original language"),
                            Map.of("key", "targetLang", "label", "Target Language", "type", "select", "required", true,
                                   "options", langOptions, "helpText", "Translation language"),
                            Map.of("key", "domain", "label", "Domain", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "general", "label", "General"),
                                       Map.of("value", "medical", "label", "Medical"),
                                       Map.of("value", "legal", "label", "Legal"),
                                       Map.of("value", "technical", "label", "Technical")
                                   ), "helpText", "Specialization for accuracy"))),
                    Map.of("actionKey", "text-to-speech", "name", "Text to Speech",
                        "description", "Convert text to speech in Indian languages",
                        "configSchema", List.of(
                            Map.of("key", "text", "label", "Text", "type", "textarea", "required", true, "helpText", "Content to speak"),
                            Map.of("key", "lang", "label", "Language", "type", "select", "required", true,
                                   "options", langOptions, "helpText", "Speech language"),
                            Map.of("key", "speaker", "label", "Voice", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "meera", "label", "Meera (Female)"),
                                       Map.of("value", "arvind", "label", "Arvind (Male)")
                                   ), "helpText", "Voice persona"))),
                    Map.of("actionKey", "chat-completion", "name", "Chat Completion",
                        "description", "Generate text using Sarvam's language model",
                        "configSchema", List.of(
                            Map.of("key", "prompt", "label", "Prompt", "type", "textarea", "required", true, "helpText", "User prompt"),
                            Map.of("key", "model", "label", "Model", "type", "text", "required", false,
                                   "placeholder", "saaras:v2", "helpText", "Model name")))
                )
        ).credentialSchema(List.of(
            Map.of("key", "apiKey", "label", "API Key", "type", "password", "required", true,
                    "placeholder", "sk-...", "helpText", "Get free credits at sarvam.ai")
        )).category("ai").helpUrl("https://www.sarvam.ai/");
    }
}