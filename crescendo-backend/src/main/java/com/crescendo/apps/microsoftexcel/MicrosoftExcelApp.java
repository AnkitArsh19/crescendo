package com.crescendo.apps.microsoftexcel;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MicrosoftExcelApp implements AppDefinition {

    @Override
    public App toApp() {
        // ── Shared field definitions ──
        var workbookField = Map.of("key", "driveItemId", "label", "Workbook",
                "type", "dynamic_dropdown", "resourceType", "workbooks",
                "required", true,
                "helpText", "Select the Excel workbook from your OneDrive");

        var worksheetField = Map.<String, Object>of("key", "worksheetId", "label", "Worksheet",
                "type", "dynamic_dropdown", "resourceType", "worksheets",
                "required", true, "dependsOn", List.of("driveItemId"),
                "helpText", "Select the worksheet within the workbook");

        var columnField = Map.<String, Object>of("key", "columnId", "label", "Column",
                "type", "dynamic_dropdown", "resourceType", "columns",
                "required", false, "dependsOn", List.of("driveItemId", "worksheetId"),
                "helpText", "Optionally filter by a specific column");

        return new App(
                "microsoft-excel",
                "Microsoft Excel",
                "Connect Excel workbooks in OneDrive — read rows, append data, and watch for changes",
                "/icons/microsoft-excel.svg",
                AuthType.OAUTH2,

                // ═══ TRIGGERS ═══
                List.of(
                    Map.of(
                        "triggerKey", "new-row",
                        "name", "New Row",
                        "description", "Triggers when a new row is added to a worksheet",
                        "configSchema", List.of(workbookField, worksheetField, columnField)
                    ),
                    Map.of(
                        "triggerKey", "updated-row",
                        "name", "New or Updated Row",
                        "description", "Triggers when a row is added or an existing row is updated",
                        "configSchema", List.of(workbookField, worksheetField, columnField)
                    ),
                    Map.of(
                        "triggerKey", "new-worksheet",
                        "name", "New Worksheet",
                        "description", "Triggers when a new worksheet is added to a workbook",
                        "configSchema", List.of(workbookField)
                    )
                ),

                // ═══ ACTIONS ═══
                List.of(
                    Map.of(
                        "actionKey", "append-row",
                        "name", "Add Row",
                        "description", "Append a new row to the end of a worksheet",
                        "configSchema", List.of(
                            workbookField, worksheetField,
                            Map.of("key", "values", "label", "Row Values",
                                   "type", "textarea", "required", true,
                                   "placeholder", "[\"Alice\", \"42\", \"alice@example.com\"]",
                                   "helpText", "JSON array of values for the new row, matching column order"))
                    ),
                    Map.of(
                        "actionKey", "update-row",
                        "name", "Update Row",
                        "description", "Update an existing row in a worksheet by row number",
                        "configSchema", List.of(
                            workbookField, worksheetField,
                            Map.of("key", "rowIndex", "label", "Row Number",
                                   "type", "text", "required", true,
                                   "placeholder", "2",
                                   "helpText", "The row number to update (1-based, where 1 is the header row)"),
                            Map.of("key", "values", "label", "New Values",
                                   "type", "textarea", "required", true,
                                   "placeholder", "[\"Bob\", \"35\", \"bob@example.com\"]",
                                   "helpText", "JSON array of updated values for the row"))
                    ),
                    Map.of(
                        "actionKey", "find-row",
                        "name", "Find Row",
                        "description", "Find a row by searching a column for a value",
                        "configSchema", List.of(
                            workbookField, worksheetField,
                            Map.<String, Object>of("key", "searchColumn", "label", "Search Column",
                                   "type", "dynamic_dropdown", "resourceType", "columns",
                                   "required", true, "dependsOn", List.of("driveItemId", "worksheetId"),
                                   "helpText", "Select which column to search"),
                            Map.of("key", "searchValue", "label", "Search Value",
                                   "type", "text", "required", true,
                                   "placeholder", "alice@example.com",
                                   "helpText", "The value to search for in the column"))
                    ),
                    Map.of(
                        "actionKey", "create-workbook",
                        "name", "Create Workbook",
                        "description", "Create a new empty Excel workbook in OneDrive",
                        "configSchema", List.of(
                            Map.of("key", "fileName", "label", "Workbook Name",
                                   "type", "text", "required", true,
                                   "placeholder", "report.xlsx",
                                   "helpText", "Name for the new workbook (must end in .xlsx)"),
                            Map.of("key", "folderId", "label", "Folder",
                                   "type", "dynamic_dropdown", "resourceType", "folders",
                                   "required", false,
                                   "helpText", "Select the OneDrive folder (default: root)"))
                    ),
                    Map.of(
                        "actionKey", "create-worksheet",
                        "name", "Create Worksheet",
                        "description", "Add a new worksheet to an existing workbook",
                        "configSchema", List.of(
                            workbookField,
                            Map.of("key", "sheetName", "label", "Worksheet Name",
                                   "type", "text", "required", true,
                                   "placeholder", "Sheet2",
                                   "helpText", "Name for the new worksheet"))
                    ),
                    Map.of(
                        "actionKey", "delete-row",
                        "name", "Delete Row",
                        "description", "Delete a row from a worksheet by row number",
                        "configSchema", List.of(
                            workbookField, worksheetField,
                            Map.of("key", "rowIndex", "label", "Row Number",
                                   "type", "text", "required", true,
                                   "placeholder", "5",
                                   "helpText", "The row number to delete (1-based)"))
                    )
                )
        )
        .credentialSchema(List.of())
        .category("productivity")
        .helpUrl("https://portal.azure.com/");
    }
}
