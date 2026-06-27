package com.crescendo.apps.googletranslate;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GoogleTranslateApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("googletranslate", "Google Translate", """
                Google Translate is a multilingual neural machine translation service developed by Google to translate text, documents and websites from one language into another.
                
                **What you can do with Google Translate in Crescendo:**
                - Automatically translate incoming emails into your preferred language
                - Localize text snippets for multi-regional support
                - Translate user feedback or social media mentions
                
                **Actions available:**
                - Language - Translate — translates a given string of text to the target language
                
                **Who should use this:** Global support teams, developers building localized features, and content creators.
                
                **Authentication:** Requires a Google Cloud API Key or OAuth2 token with Cloud Translation API enabled.
                """,
                "https://www.google.com/s2/favicons?domain=translate.google.com&sz=128", AuthType.OAUTH2,
                List.of(),
                List.of(
                    Map.of(
                            "actionKey", "translate",
                            "name", "Language - Translate",
                            "description", "Translate a language",
                            "configSchema", List.of(
                                    Map.of("key", "text", "label", "Text", "type", "textarea", "required", true, "description", "The input text to translate"),
                                    Map.of("key", "translateTo", "label", "Translate To", "type", "text", "required", true, "description", "The language code to translate to (e.g. 'es', 'fr')")
                            )
                    )
                )
        ).credentialSchema(List.of(
            Map.of("key", "apiKey", "label", "API Key", "type", "password", "required", false),
            Map.of("key", "accessToken", "label", "OAuth2 Token", "type", "password", "required", false)
        )).category("productivity");
    }
}
