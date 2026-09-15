package com.crescendo.apps.jobsearch;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JobSearchAggregateHandlerTest {

    private final JobSearchAggregateHandler handler = new JobSearchAggregateHandler();

    private ActionContext createContext(Map<String, Object> config) {
        return new ActionContext(
                "job-search",
                "search-jobs",
                config,
                Map.of(),
                Map.of(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                1
        );
    }

    @Test
    @DisplayName("execute fails when query is missing or blank")
    void execute_failsWhenQueryMissing() {
        ActionContext missing = createContext(Map.of());
        ActionResult res1 = handler.execute(missing);
        assertFalse(res1.success());
        assertTrue(res1.error().contains("'query' is required"));

        ActionContext empty = createContext(Map.of("query", "   "));
        ActionResult res2 = handler.execute(empty);
        assertFalse(res2.success());
        assertTrue(res2.error().contains("'query' is required"));
    }

    @Test
    @DisplayName("JobSearchApp registers search-jobs action with internshipOnly and ashby config")
    void appDefinition_registersConfigFields() {
        JobSearchApp app = new JobSearchApp();
        var actions = app.toApp().getActions();
        assertNotNull(actions);
        assertFalse(actions.isEmpty());

        var searchAction = actions.stream()
                .filter(a -> "search-jobs".equals(a.get("actionKey")))
                .findFirst()
                .orElse(null);
        assertNotNull(searchAction);

        @SuppressWarnings("unchecked")
        var schema = (java.util.List<Map<String, Object>>) searchAction.get("configSchema");
        assertNotNull(schema);

        boolean hasInternshipOnly = schema.stream().anyMatch(field -> "internshipOnly".equals(field.get("key")));
        boolean hasAshbyBoards = schema.stream().anyMatch(field -> "ashbyBoardSlugs".equals(field.get("key")));
        boolean hasGreenhouseBoards = schema.stream().anyMatch(field -> "greenhouseBoardTokens".equals(field.get("key")));
        boolean hasLeverBoards = schema.stream().anyMatch(field -> "leverBoardSlugs".equals(field.get("key")));
        boolean hasTargetOnly = schema.stream().anyMatch(field -> "targetCompaniesOnly".equals(field.get("key")));
        boolean hasTargetCategories = schema.stream().anyMatch(field -> "targetCompanyCategories".equals(field.get("key")));
        boolean hasCustomTarget = schema.stream().anyMatch(field -> "customTargetCompanies".equals(field.get("key")));

        assertTrue(hasInternshipOnly, "configSchema must contain internshipOnly field");
        assertTrue(hasAshbyBoards, "configSchema must contain ashbyBoardSlugs field");
        assertTrue(hasGreenhouseBoards, "configSchema must contain greenhouseBoardTokens field");
        assertTrue(hasLeverBoards, "configSchema must contain leverBoardSlugs field");
        assertTrue(hasTargetOnly, "configSchema must contain targetCompaniesOnly field");
        assertTrue(hasTargetCategories, "configSchema must contain targetCompanyCategories field");
        assertTrue(hasCustomTarget, "configSchema must contain customTargetCompanies field");
    }

    @Test
    @DisplayName("parseTags correctly parses arrays, bracketed strings, and comma-separated lists")
    void parseTags_handlesAllInputFormats() throws Exception {
        var method = JobSearchAggregateHandler.class.getDeclaredMethod("parseTags", Object.class);
        method.setAccessible(true);

        // 1. List input
        @SuppressWarnings("unchecked")
        var fromList = (java.util.List<String>) method.invoke(handler, java.util.List.of("Intern", "Software Engineer Intern", "apprentice"));
        assertEquals(java.util.List.of("Intern", "Software Engineer Intern", "apprentice"), fromList);

        // 2. Bracketed string input from UI
        @SuppressWarnings("unchecked")
        var fromBracketed = (java.util.List<String>) method.invoke(handler, "[Intern, Software Engineer Intern, apprentice]");
        assertEquals(java.util.List.of("Intern", "Software Engineer Intern", "apprentice"), fromBracketed);

        // 3. Comma-separated string
        @SuppressWarnings("unchecked")
        var fromComma = (java.util.List<String>) method.invoke(handler, "Intern, Software Engineer Intern, apprentice");
        assertEquals(java.util.List.of("Intern", "Software Engineer Intern", "apprentice"), fromComma);
    }
}
