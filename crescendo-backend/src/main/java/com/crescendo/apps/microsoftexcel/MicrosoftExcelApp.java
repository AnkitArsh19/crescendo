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
                "Microsoft Excel", """
                Microsoft Excel is a spreadsheet developed by Microsoft. The Crescendo Excel app connects to workbooks in your OneDrive, allowing you to read rows, append data, and watch for changes automatically.

                **What you can do with Excel in Crescendo:**
                - Append new rows for incoming form submissions or webhook data
                - Trigger workflows when a specific row is updated
                - Create monthly report workbooks automatically
                - Sync Excel data with databases like PostgreSQL or Airtable

                **Triggers available:**
                - New Row — trigger workflows when data is appended
                - Updated Row — detect changes in existing data
                - New Worksheet — trigger when a sheet is added

                **Actions available:**
                - Add Row / Update Row / Delete Row — modify data
                - Find Row — search a column for a specific value
                - Create Workbook/Worksheet — manage your files

                **Who should use this:** Finance teams tracking budgets, data analysts, and enterprise users managing records in Office 365.

                **Authentication:** OAuth 2.0 (connect your Microsoft account).
                """,
                "https://upload.wikimedia.org/wikipedia/commons/e/e3/Microsoft_Office_Excel_%282019%E2%80%932025%29.svg",
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
                        "actionKey", "appendRow",
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
                        "actionKey", "updateRow",
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
                        "actionKey", "findRow",
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
                        "actionKey", "createWorkbook",
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
                        "actionKey", "createWorksheet",
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
                        "actionKey", "deleteRow",
                        "name", "Delete Row",
                        "description", "Delete a row from a worksheet by row number",
                        "configSchema", List.of(
                            workbookField, worksheetField,
                            Map.of("key", "rowIndex", "label", "Row Number",
                                   "type", "text", "required", true,
                                   "placeholder", "5",
                                   "helpText", "The row number to delete (1-based)"))
                    ),
                    Map.of(
                        "actionKey", "readRows",
                        "name", "Read Rows",
                        "description", "Read data from a worksheet or a specific range",
                        "configSchema", List.of(
                            workbookField, worksheetField,
                            Map.of("key", "range", "label", "Range",
                                   "type", "text", "required", false,
                                   "placeholder", "A1:B10",
                                   "helpText", "The sheet range to read data from (e.g. A1:B10). Leave blank to return entire used range."),
                            Map.of("key", "keyRow", "label", "Header Row",
                                   "type", "text", "required", false,
                                   "placeholder", "0",
                                   "helpText", "Index of the row which contains column names (0-based relative to Range). Default: 0"),
                            Map.of("key", "dataStartRow", "label", "First Data Row",
                                   "type", "text", "required", false,
                                   "placeholder", "1",
                                   "helpText", "Index of first row which contains actual data (0-based relative to Range). Default: 1")
                        )
                    )
                )
        )
        .credentialSchema(List.of())
        .category("productivity")
        .helpUrl("https://portal.azure.com/");
    }
}
