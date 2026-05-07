package com.crescendo.apps.googledocs;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GoogleDocsApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("google-docs", "Google Docs", "Create documents and write content into Google Docs",
                "/icons/google-docs.svg", AuthType.OAUTH2,
                List.of(Map.of(
                    "triggerKey", "document-updated",
                    "name", "Document Updated",
                    "description", "Triggers when a Google Doc changes",
                    "configSchema", List.of(
                        Map.of("key", "documentId", "label", "Document",
                               "type", "dynamic_dropdown", "resourceType", "documents",
                               "required", true,
                               "helpText", "Select the document to watch for changes")
                    )
                )),
                List.of(
                    Map.of(
                        "actionKey", "create-document",
                        "name", "Create Document",
                        "description", "Create a new Google Doc",
                        "configSchema", List.of(
                            Map.of("key", "title", "label", "Document Title",
                                   "type", "text", "required", true,
                                   "placeholder", "Meeting Notes",
                                   "helpText", "Title of the new document")
                        )
                    ),
                    Map.of(
                        "actionKey", "append-text",
                        "name", "Append Text",
                        "description", "Append text to the end of a Google Doc",
                        "configSchema", List.of(
                            Map.of("key", "documentId", "label", "Document",
                                   "type", "dynamic_dropdown", "resourceType", "documents",
                                   "required", true,
                                   "helpText", "Select the document to append to"),
                            Map.of("key", "text", "label", "Text",
                                   "type", "textarea", "required", true,
                                   "placeholder", "Your text here...",
                                   "helpText", "Text to append to the document")
                        )
                    )
                ))
                .credentialSchema(List.of())
                .category("productivity")
                .helpUrl("https://console.cloud.google.com/");
    }
}