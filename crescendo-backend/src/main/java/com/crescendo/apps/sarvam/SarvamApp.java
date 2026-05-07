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
        return new App("sarvam", "Sarvam AI", "Indian language AI — translation, TTS, and speech-to-text",
                "/icons/sarvam.svg", AuthType.APIKEY,
                List.of(),
                List.of(
                    Map.of(
                        "actionKey", "translate",
                        "name", "Translate Text",
                        "description", "Translate text between Indian languages",
                        "configSchema", Map.of(
                            "text", "string (required) — text to translate",
                            "sourceLang", "string (required) — source language code",
                            "targetLang", "string (required) — target language code"
                        )
                    ),
                    Map.of(
                        "actionKey", "text-to-speech",
                        "name", "Text to Speech",
                        "description", "Convert text to speech in Indian languages",
                        "configSchema", Map.of(
                            "text", "string (required) — text to speak",
                            "lang", "string (required) — language code"
                        )
                    )
                )
        )
        .credentialSchema(List.of(
            Map.of("key", "apiKey", "label", "API Key",
                    "type", "password", "required", true,
                    "placeholder", "sk-...",
                    "helpText", "Sign up at sarvam.ai and get free starter credits. Find your API key in the dashboard.")
        ))
        .category("ai")
        .helpUrl("https://www.sarvam.ai/");
    }
}