package com.crescendo.apps.googledrive;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GoogleDriveApp implements AppDefinition {

    @Override
    public App toApp() {
        var folderField = Map.of("key", "folderId", "label", "Folder",
                "type", "dynamic_dropdown", "resourceType", "folders",
                "required", false,
                "helpText", "Select a folder (leave empty for all / root)");

        return new App("google-drive", "Google Drive", "Upload files, manage folders, and watch for changes in Google Drive",
                "/icons/google-drive.svg", AuthType.OAUTH2,

                // ═══ TRIGGERS ═══
                List.of(
                    Map.of(
                        "triggerKey", "new-file",
                        "name", "New File",
                        "description", "Triggers when a new file is uploaded to a folder",
                        "configSchema", List.of(folderField)
                    ),
                    Map.of(
                        "triggerKey", "new-folder",
                        "name", "New Folder",
                        "description", "Triggers when a new subfolder is created",
                        "configSchema", List.of(folderField)
                    ),
                    Map.of(
                        "triggerKey", "updated-file",
                        "name", "Updated File",
                        "description", "Triggers when a file is modified",
                        "configSchema", List.of(folderField)
                    )
                ),

                // ═══ ACTIONS ═══
                List.of(
                    Map.of(
                        "actionKey", "upload-file",
                        "name", "Upload File",
                        "description", "Upload a file to Google Drive",
                        "configSchema", List.of(
                            Map.of("key", "folderId", "label", "Destination Folder",
                                   "type", "dynamic_dropdown", "resourceType", "folders",
                                   "required", false,
                                   "helpText", "Select the folder to upload to (default: root)"),
                            Map.of("key", "fileName", "label", "File Name",
                                   "type", "text", "required", true,
                                   "placeholder", "report.txt",
                                   "helpText", "Name for the uploaded file"),
                            Map.of("key", "content", "label", "File Content",
                                   "type", "textarea", "required", true,
                                   "helpText", "The file content or a URL to fetch")
                        )
                    ),
                    Map.of(
                        "actionKey", "create-folder",
                        "name", "Create Folder",
                        "description", "Create a new folder in Google Drive",
                        "configSchema", List.of(
                            Map.of("key", "parentFolderId", "label", "Parent Folder",
                                   "type", "dynamic_dropdown", "resourceType", "folders",
                                   "required", false,
                                   "helpText", "Select parent folder (default: root)"),
                            Map.of("key", "folderName", "label", "Folder Name",
                                   "type", "text", "required", true,
                                   "placeholder", "Reports/Q1",
                                   "helpText", "Name for the new folder")
                        )
                    ),
                    Map.of(
                        "actionKey", "list-files",
                        "name", "List Files",
                        "description", "List files in a Google Drive folder",
                        "configSchema", List.of(
                            folderField,
                            Map.of("key", "pageSize", "label", "Max Results",
                                   "type", "text", "required", false,
                                   "placeholder", "20",
                                   "helpText", "Maximum files to return")
                        )
                    ),
                    Map.of(
                        "actionKey", "move-file",
                        "name", "Move File",
                        "description", "Move a file to a different folder",
                        "configSchema", List.of(
                            Map.of("key", "fileId", "label", "File",
                                   "type", "dynamic_dropdown", "resourceType", "files",
                                   "required", true,
                                   "helpText", "Select the file to move"),
                            Map.of("key", "destinationFolderId", "label", "Destination Folder",
                                   "type", "dynamic_dropdown", "resourceType", "folders",
                                   "required", true,
                                   "helpText", "Select the folder to move the file to")
                        )
                    )
                )
        )
        .credentialSchema(List.of())
        .category("productivity")
        .helpUrl("https://console.cloud.google.com/");
    }
}
