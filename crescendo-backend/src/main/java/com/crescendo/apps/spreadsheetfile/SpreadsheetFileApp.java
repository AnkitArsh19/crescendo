package com.crescendo.apps.spreadsheetfile;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class SpreadsheetFileApp implements AppDefinition {
    @Override public App toApp() {
        return new App("spreadsheet-file", "Spreadsheet File", "Read and write CSV, XLS, XLSX, and simple ODS spreadsheet data",
                "/icons/spreadsheet.svg", AuthType.NONE, List.of(), List.of(
                Map.of("actionKey","read","name","Read Spreadsheet","description","Read CSV, XLS, XLSX, or ODS Base64 data into rows",
                        "configSchema", List.of(
                                Map.of("key","format","label","Format","type","select","required",true,"options",List.of(Map.of("value","csv","label","CSV"),Map.of("value","xls","label","XLS"),Map.of("value","xlsx","label","XLSX"),Map.of("value","ods","label","ODS"))),
                                Map.of("key","base64","label","File Base64","type","textarea","required",true),
                                Map.of("key","sheetName","label","Sheet Name","type","text","required",false))),
                Map.of("actionKey","write","name","Write Spreadsheet","description","Write rows to CSV, XLS, XLSX, or ODS Base64 data",
                        "configSchema", List.of(
                                Map.of("key","format","label","Format","type","select","required",true,"options",List.of(Map.of("value","csv","label","CSV"),Map.of("value","xls","label","XLS"),Map.of("value","xlsx","label","XLSX"),Map.of("value","ods","label","ODS"))),
                                Map.of("key","rows","label","Rows (JSON Array)","type","json","required",true),
                                Map.of("key","sheetName","label","Sheet Name","type","text","required",false,"placeholder","Sheet1")))
        )).credentialSchema(List.of()).category("core").helpUrl("");
    }
}
