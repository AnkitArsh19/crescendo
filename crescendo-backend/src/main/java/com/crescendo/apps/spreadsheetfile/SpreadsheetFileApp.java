package com.crescendo.apps.spreadsheetfile;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for Spreadsheet File.
 */
@Component
public class SpreadsheetFileApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "spreadsheetFile",
                "Spreadsheet File",
                """
                Reads and writes data from a spreadsheet file like CSV, XLS, ODS, etc.
                
                This integration provides operations for:
                - **Read From File**: Read data from a spreadsheet file
                - **Write to File**: Write data to a spreadsheet file
                """,
                "https://www.google.com/s2/favicons?domain=excel.com&sz=128", // Generic icon
                AuthType.NONE,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "spreadsheetFile:fromFile",
                                "name", "Read From File",
                                "description", "Read data from a spreadsheet file",
                                "configSchema", List.of(
                                        Map.of("key", "fileFormat", "label", "File Format", "type", "text", "default", "autodetect"),
                                        Map.of("key", "binaryPropertyName", "label", "Input Binary Field", "type", "text", "required", true, "default", "data"),
                                        Map.of("key", "options", "label", "Options", "type", "json")
                                )
                        ),
                        Map.of(
                                "actionKey", "spreadsheetFile:toFile",
                                "name", "Write to File",
                                "description", "Write data to a spreadsheet file",
                                "configSchema", List.of(
                                        Map.of("key", "fileFormat", "label", "File Format", "type", "text", "default", "csv"),
                                        Map.of("key", "binaryPropertyName", "label", "Output Binary Field", "type", "text", "required", true, "default", "data"),
                                        Map.of("key", "options", "label", "Options", "type", "json")
                                )
                        )
                )
        ).credentialSchema(List.of()).category("files-and-storage").internal(true);
    }
}
