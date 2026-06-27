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

        var fileField = Map.of("key", "fileId", "label", "File",
                "type", "dynamic_dropdown", "resourceType", "files",
                "required", true,
                "helpText", "Select a file");

        return new App("google-drive", "Google Drive", """
                Google Drive is a file storage and synchronization service. The Crescendo Google Drive app allows you to automatically upload files, manage folders, and watch for document changes.

                **What you can do with Google Drive in Crescendo:**
                - Save email attachments directly to specific Drive folders
                - Trigger a notification when a new file is uploaded to a shared folder
                - Automatically generate standard folder structures for new clients
                - Sync files uploaded in Discord or Slack to Drive for permanent storage

                **Triggers available:**
                - New File in Folder — trigger a workflow when a file is added

                **Actions available:**
                - Upload File — save a file to a specific location
                - Create Folder — generate new directories
                - Find File/Folder — search your Drive by name or type

                **Who should use this:** Teams managing digital assets, HR departments handling employee documents, and freelancers organizing client files.

                **Authentication:** OAuth 2.0 (connect your Google account).
                """,
                "https://ssl.gstatic.com/images/branding/product/2x/drive_2020q4_48dp.png", AuthType.OAUTH2,

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
                        "actionKey", "upload",
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
                            Map.of("key", "file", "label", "Upload File",
                                   "type", "file", "required", false,
                                   "accept", "*/*", "maxSizeMB", 25,
                                   "helpText", "Select a file to upload (or use text content below)"),
                            Map.of("key", "content", "label", "Text Content (Alternative)",
                                   "type", "textarea", "required", false,
                                   "helpText", "Paste text content or a URL — used when no file is uploaded")
                        )
                    ),
                    Map.of(
                        "actionKey", "createFolder",
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
                        "actionKey", "getAll",
                        "name", "List Files",
                        "description", "List files in a Google Drive folder",
                        "configSchema", List.of(
                            folderField,
                            Map.of("key", "query", "label", "Search Query",
                                   "type", "text", "required", false,
                                   "placeholder", "name contains 'report'",
                                   "helpText", "Google Drive search query (e.g., name contains 'report')"),
                            Map.of("key", "pageSize", "label", "Max Results",
                                   "type", "text", "required", false,
                                   "placeholder", "20",
                                   "helpText", "Maximum files to return")
                        )
                    ),
                    Map.of(
                        "actionKey", "move",
                        "name", "Move File",
                        "description", "Move a file to a different folder",
                        "configSchema", List.of(
                            fileField,
                            Map.of("key", "destinationFolderId", "label", "Destination Folder",
                                   "type", "dynamic_dropdown", "resourceType", "folders",
                                   "required", true,
                                   "helpText", "Select the folder to move the file to")
                        )
                    ),
                    Map.of(
                        "actionKey", "copy",
                        "name", "Copy File",
                        "description", "Create a copy of a file",
                        "configSchema", List.of(
                            fileField,
                            Map.of("key", "newName", "label", "New Name",
                                   "type", "text", "required", false,
                                   "placeholder", "Copy of report.txt",
                                   "helpText", "Name for the copy (leave blank to keep original name)"),
                            Map.of("key", "destinationFolderId", "label", "Destination Folder",
                                   "type", "dynamic_dropdown", "resourceType", "folders",
                                   "required", false,
                                   "helpText", "Optionally place the copy in a different folder")
                        )
                    ),
                    Map.of(
                        "actionKey", "deleteFile",
                        "name", "Delete File",
                        "description", "Permanently delete a file from Google Drive",
                        "configSchema", List.of(fileField)
                    )
                )
        )
        .credentialSchema(List.of())
        .category("productivity")
        .helpUrl("https://console.cloud.google.com/");
    }
}
