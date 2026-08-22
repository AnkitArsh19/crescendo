package com.crescendo.apps.googlesheets;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GoogleSheetsSheetHandlersTest {

    private final GoogleSheetsSheetHandlers handlers = new GoogleSheetsSheetHandlers();

    private ActionContext createContext(Map<String, Object> config, Map<String, Object> credentials) {
        return new ActionContext("google-sheets", "appendRow", config, credentials, Map.of(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1);
    }

    @Test
    @DisplayName("appendRow fails when accessToken is missing")
    void appendRow_failsWhenTokenMissing() {
        ActionContext context = createContext(Map.of("spreadsheetId", "sheet-123", "range", "Sheet1!A1", "values", List.of("A", "B")), Map.of());
        ActionResult result = handlers.appendRow(context);

        assertFalse(result.success());
        assertTrue(result.error().contains("accessToken"));
    }

    @Test
    @DisplayName("appendRow fails when spreadsheetId is missing")
    void appendRow_failsWhenSpreadsheetIdMissing() {
        ActionContext context = createContext(Map.of("range", "Sheet1!A1", "values", List.of("A", "B")), Map.of("accessToken", "valid-token"));
        ActionResult result = handlers.appendRow(context);

        assertFalse(result.success());
        assertTrue(result.error().contains("spreadsheetId"));
    }

    @Test
    @DisplayName("appendRow fails when range is missing")
    void appendRow_failsWhenRangeMissing() {
        ActionContext context = createContext(Map.of("spreadsheetId", "sheet-123", "values", List.of("A", "B")), Map.of("accessToken", "valid-token"));
        ActionResult result = handlers.appendRow(context);

        assertFalse(result.success());
        assertTrue(result.error().contains("range"));
    }

    @Test
    @DisplayName("appendRow fails when values are missing")
    void appendRow_failsWhenValuesMissing() {
        ActionContext context = createContext(Map.of("spreadsheetId", "sheet-123", "range", "Sheet1!A1"), Map.of("accessToken", "valid-token"));
        ActionResult result = handlers.appendRow(context);

        assertFalse(result.success());
        assertTrue(result.error().contains("values"));
    }
}
