package com.crescendo.apps.mock;

import com.crescendo.apps.catfacts.CatFactsGetFactHandler;
import com.crescendo.apps.datetime.DateTimeHandlers;
import com.crescendo.apps.jokeapi.JokeApiGetJokeHandler;
import com.crescendo.apps.json.JsonActionHandler;
import com.crescendo.apps.markdown.MarkdownHandlers;
import com.crescendo.apps.quotes.QuotesRandomHandler;
import com.crescendo.apps.renamekeys.RenameKeysHandlers;
import com.crescendo.apps.set.SetHandlers;
import com.crescendo.apps.totp.TotpHandlers;
import com.crescendo.apps.xml.XmlHandlers;
import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UtilityAppsMockTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ActionContext createContext(String appKey, String actionKey, Map<String, Object> config, Map<String, Object> input) {
        return new ActionContext(appKey, actionKey, config, Map.of(), input != null ? input : Map.of(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1);
    }

    // ── Date & Time ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("DateTime: getCurrentDate outputs formatted ISO date and epoch timestamp")
    void testDateTime_getCurrentDate() throws Exception {
        DateTimeHandlers handlers = new DateTimeHandlers();
        ActionContext context = createContext("dateTime", "dateTime:getCurrentDate", Map.of("outputFieldName", "currentDate"), Map.of());
        
        Object result = handlers.getCurrentDate(context);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertNotNull(map.get("currentDate"));
        assertNotNull(map.get("timestamp"));
    }

    // ── JSON, XML & Markdown ──────────────────────────────────────────────────

    @Test
    @DisplayName("JSON: parse parses valid JSON string into Map")
    void testJson_parse() {
        JsonActionHandler handler = new JsonActionHandler(objectMapper);
        ActionContext context = createContext("json", "parse", Map.of("propertyName", "rawJson", "destinationProperty", "parsed"), Map.of("rawJson", "{\"name\":\"Crescendo\",\"active\":true}"));
        
        ActionResult result = handler.execute(context);
        assertTrue(result.success());
        assertTrue(result.outputData().containsKey("parsed"));
    }

    @Test
    @DisplayName("JSON: stringify serializes Map into JSON string")
    void testJson_stringify() {
        JsonActionHandler handler = new JsonActionHandler(objectMapper);
        ActionContext context = createContext("json", "stringify", Map.of("propertyName", "dataObj", "destinationProperty", "jsonString"), Map.of("dataObj", Map.of("score", 100)));
        
        ActionResult result = handler.execute(context);
        assertTrue(result.success());
        assertTrue(result.outputData().containsKey("jsonString"));
    }

    @Test
    @DisplayName("XML: convert converts mode successfully")
    void testXml_convert() throws Exception {
        XmlHandlers handlers = new XmlHandlers();
        ActionContext context = createContext("xml", "xml:convert", Map.of("mode", "xmlToJson"), Map.of());
        
        Object result = handlers.convertXmlJson(context);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("success", map.get("status"));
        assertEquals("xmlToJson", map.get("mode"));
    }

    @Test
    @DisplayName("Markdown: convert returns converted placeholder without errors")
    void testMarkdown_convert() throws Exception {
        MarkdownHandlers handlers = new MarkdownHandlers();
        ActionContext context = createContext("markdown", "markdown:convert", Map.of("mode", "markdownToHtml", "destinationKey", "htmlResult"), Map.of());
        
        Object result = handlers.convert(context);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("success", map.get("status"));
        assertTrue(map.containsKey("htmlResult"));
    }

    // ── Flow & State Manipulation ─────────────────────────────────────────────

    @Test
    @DisplayName("Set: set merges fields onto context inputData")
    void testSet_set() throws Exception {
        SetHandlers handlers = new SetHandlers();
        ActionContext context = createContext("set", "set:set", Map.of("mode", "manual"), Map.of("initialKey", "value123"));
        
        Object result = handlers.set(context);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("value123", map.get("initialKey"));
    }

    @Test
    @DisplayName("RenameKeys: rename returns success confirmation")
    void testRenameKeys_rename() throws Exception {
        RenameKeysHandlers handlers = new RenameKeysHandlers();
        ActionContext context = createContext("renameKeys", "renameKeys:rename", Map.of(), Map.of("oldKey", "test"));
        
        Object result = handlers.rename(context);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("success", map.get("status"));
    }

    @Test
    @DisplayName("Totp: generateSecret returns TOTP metadata")
    void testTotp_generateSecret() throws Exception {
        TotpHandlers handlers = new TotpHandlers();
        ActionContext context = createContext("totp", "totp:generateSecret", Map.of(), Map.of());
        
        Object result = handlers.generateSecret(context);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("success", map.get("status"));
        assertTrue(map.containsKey("secondsRemaining"));
    }
}
