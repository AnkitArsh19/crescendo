package com.crescendo.apps.googlesheets;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GoogleSheetsApp implements AppDefinition {

    @Override
    public App toApp() {
        var spreadsheetField = Map.of("key", "spreadsheetId", "label", "Spreadsheet",
                "type", "dynamic_dropdown", "resourceType", "spreadsheets",
                "required", true,
                "helpText", "Select a spreadsheet from your Google account");

        var worksheetField = Map.<String, Object>of("key", "sheetName", "label", "Worksheet",
                "type", "dynamic_dropdown", "resourceType", "worksheets",
                "dependsOn", List.of("spreadsheetId"),
                "required", true,
                "helpText", "Select the worksheet within the spreadsheet");

        var columnField = Map.<String, Object>of("key", "triggerColumn", "label", "Trigger Column",
                "type", "dynamic_dropdown", "resourceType", "columns",
                "dependsOn", List.of("spreadsheetId", "sheetName"),
                "required", false,
                "helpText", "Optionally limit trigger to changes in a specific column");

        return new App("google-sheets", "Google Sheets", "Read, write, and watch spreadsheet data",
                "/icons/google-sheets.svg", AuthType.OAUTH2,

                // ═══ TRIGGERS ═══
                List.of(
                    Map.of(
                        "triggerKey", "new-row",
                        "name", "New or Updated Row",
                        "description", "Triggers when a new row is added or an existing row is updated",
                        "configSchema", List.of(spreadsheetField, worksheetField, columnField)
                    ),
                    Map.of(
                        "triggerKey", "new-worksheet",
                        "name", "New Worksheet",
                        "description", "Triggers when a new worksheet is added to a spreadsheet",
                        "configSchema", List.of(spreadsheetField)
                    )
                ),

                // ═══ ACTIONS ═══
                List.of(
                    Map.of(
                        "actionKey", "append-row",
                        "name", "Append Row",
                        "description", "Add a new row to the end of a spreadsheet",
                        "configSchema", List.of(
                            spreadsheetField, worksheetField,
                            Map.of("key", "values", "label", "Row Values",
                                   "type", "array", "required", true,
                                   "helpText", "Comma-separated values for the new row")
                        )
                    ),
                    Map.of(
                        "actionKey", "update-row",
                        "name", "Update Row",
                        "description", "Update an existing row by row number",
                        "configSchema", List.of(
                            spreadsheetField, worksheetField,
                            Map.of("key", "rowIndex", "label", "Row Number",
                                   "type", "text", "required", true,
                                   "placeholder", "2",
                                   "helpText", "The row number to update (1-based, where 1 = header)"),
                            Map.of("key", "values", "label", "New Values",
                                   "type", "array", "required", true,
                                   "helpText", "Updated values for the row")
                        )
                    ),
                    Map.of(
                        "actionKey", "read-rows",
                        "name", "Read Rows",
                        "description", "Read rows from a spreadsheet range",
                        "configSchema", List.of(
                            spreadsheetField, worksheetField,
                            Map.of("key", "range", "label", "Range",
                                   "type", "text", "required", false,
                                   "placeholder", "A1:Z100",
                                   "helpText", "Optional cell range (e.g. A1:Z100). Omit to read all rows.")
                        )
                    ),
                    Map.of(
                        "actionKey", "find-row",
                        "name", "Find Row",
                        "description", "Search for a row by column value",
                        "configSchema", List.of(
                            spreadsheetField, worksheetField,
                            Map.<String, Object>of("key", "searchColumn", "label", "Search Column",
                                   "type", "dynamic_dropdown", "resourceType", "columns",
                                   "dependsOn", List.of("spreadsheetId", "sheetName"),
                                   "required", true,
                                   "helpText", "Select which column to search"),
                            Map.of("key", "searchValue", "label", "Search Value",
                                   "type", "text", "required", true,
                                   "placeholder", "alice@example.com",
                                   "helpText", "The value to search for in the column")
                        )
                    ),
                    Map.of(
                        "actionKey", "delete-row",
                        "name", "Delete Row",
                        "description", "Delete a row from a worksheet by row number",
                        "configSchema", List.of(
                            spreadsheetField, worksheetField,
                            Map.of("key", "rowIndex", "label", "Row Number",
                                   "type", "text", "required", true,
                                   "placeholder", "5",
                                   "helpText", "The row number to delete (1-based)")
                        )
                    ),
                    Map.of(
                        "actionKey", "create-spreadsheet",
                        "name", "Create Spreadsheet",
                        "description", "Create a new Google Sheets spreadsheet",
                        "configSchema", List.of(
                            Map.of("key", "title", "label", "Spreadsheet Title",
                                   "type", "text", "required", true,
                                   "placeholder", "Q1 Report",
                                   "helpText", "Title for the new spreadsheet")
                        )
                    )
                )
        )
        .credentialSchema(List.of())
        .category("productivity")
        .helpUrl("https://console.cloud.google.com/");
    }
}
