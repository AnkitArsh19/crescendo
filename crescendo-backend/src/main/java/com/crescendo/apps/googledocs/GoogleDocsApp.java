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
        var documentField = Map.of("key", "documentId", "label", "Document",
                "type", "dynamic_dropdown", "resourceType", "documents",
                "required", true,
                "helpText", "Select the Google Doc");

        return new App("google-docs", "Google Docs", "Create documents, append text, and search-replace content",
                "https://ssl.gstatic.com/images/branding/product/2x/docs_2020q4_48dp.png", AuthType.OAUTH2,

                // ═══ TRIGGERS ═══
                List.of(
                    Map.of(
                        "triggerKey", "new-document",
                        "name", "New Document",
                        "description", "Triggers when a new Google Doc is created",
                        "configSchema", List.of(
                            Map.of("key", "folderId", "label", "Folder",
                                   "type", "dynamic_dropdown", "resourceType", "folders",
                                   "required", false,
                                   "helpText", "Optionally watch only a specific Drive folder")
                        )
                    ),
                    Map.of(
                        "triggerKey", "document-updated",
                        "name", "Document Updated",
                        "description", "Triggers when a Google Doc changes",
                        "configSchema", List.of(documentField)
                    )
                ),

                // ═══ ACTIONS ═══
                List.of(
                    Map.of(
                        "actionKey", "create-document",
                        "name", "Create Document",
                        "description", "Create a new Google Doc",
                        "configSchema", List.of(
                            Map.of("key", "title", "label", "Document Title",
                                   "type", "text", "required", true,
                                   "placeholder", "Meeting Notes",
                                   "helpText", "Title of the new document"),
                            Map.of("key", "body", "label", "Initial Content",
                                   "type", "textarea", "required", false,
                                   "placeholder", "Start typing here...",
                                   "helpText", "Optional initial text content for the document")
                        )
                    ),
                    Map.of(
                        "actionKey", "append-text",
                        "name", "Append Text to Document",
                        "description", "Append text to the end of a Google Doc",
                        "configSchema", List.of(
                            documentField,
                            Map.of("key", "text", "label", "Text",
                                   "type", "textarea", "required", true,
                                   "placeholder", "Your text here...",
                                   "helpText", "Text to append to the document")
                        )
                    ),
                    Map.of(
                        "actionKey", "get-document",
                        "name", "Get Document",
                        "description", "Read the full text content of a Google Doc",
                        "configSchema", List.of(documentField)
                    ),
                    Map.of(
                        "actionKey", "replace-text",
                        "name", "Find and Replace Text",
                        "description", "Search and replace text across the entire document",
                        "configSchema", List.of(
                            documentField,
                            Map.of("key", "searchText", "label", "Find Text",
                                   "type", "text", "required", true,
                                   "placeholder", "{{placeholder}}",
                                   "helpText", "The text to search for in the document"),
                            Map.of("key", "replaceText", "label", "Replace With",
                                   "type", "text", "required", true,
                                   "placeholder", "Actual value",
                                   "helpText", "The replacement text"),
                            Map.of("key", "matchCase", "label", "Case Sensitive",
                                   "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("label", "No", "value", "false"),
                                       Map.of("label", "Yes", "value", "true")
                                   ),
                                   "helpText", "Whether the search should be case-sensitive")
                        )
                    )
                ))
                .credentialSchema(List.of())
                .category("productivity")
                .helpUrl("https://console.cloud.google.com/");
    }
}