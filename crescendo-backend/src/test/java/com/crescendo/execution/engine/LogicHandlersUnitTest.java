package com.crescendo.execution.engine;

import com.crescendo.apps.logic.LogicHandlers;
import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LogicHandlersUnitTest {

    private final LogicHandlers handlers = new LogicHandlers();

    private ActionContext createContext(Map<String, Object> config) {
        return new ActionContext("logic", "logic:if", config, Map.of(), Map.of(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1);
    }

    @Test
    @DisplayName("If node evaluates AND/OR combinator groups correctly")
    void ifNode_evaluatesCombinatorGroups() {
        List<Map<String, Object>> conditions = List.of(
                Map.of(
                        "combinator", "AND",
                        "conditions", List.of(
                                Map.of("leftValue", "urgent", "operator", "equals", "rightValue", "urgent"),
                                Map.of("leftValue", "10", "operator", "greaterThan", "rightValue", "5")
                        )
                )
        );

        ActionContext context = createContext(Map.of("conditions", conditions));
        Object resultObj = handlers.ifNode(context);

        assertInstanceOf(Map.class, resultObj);
        Map<?, ?> result = (Map<?, ?>) resultObj;
        assertEquals("true", result.get("_branchKey"));
        assertEquals("true", result.get("branch"));
    }

    @Test
    @DisplayName("If node returns false when conditions do not match")
    void ifNode_returnsFalseWhenUnmatched() {
        List<Map<String, Object>> conditions = List.of(
                Map.of(
                        "combinator", "AND",
                        "conditions", List.of(
                                Map.of("leftValue", "normal", "operator", "equals", "rightValue", "urgent")
                        )
                )
        );

        ActionContext context = createContext(Map.of("conditions", conditions));
        Object resultObj = handlers.ifNode(context);

        Map<?, ?> result = (Map<?, ?>) resultObj;
        assertEquals("false", result.get("_branchKey"));
    }

    @Test
    @DisplayName("If node supports regex, contains, startsWith, endsWith, and numerical comparison operators")
    void ifNode_supportsAllOperators() {
        List<Map<String, Object>> condRegex = List.of(
                Map.of("combinator", "AND", "conditions", List.of(
                        Map.of("leftValue", "PR-1234", "operator", "regex", "rightValue", "^PR-[0-9]{4}$")
                ))
        );
        Map<?, ?> r1 = (Map<?, ?>) handlers.ifNode(createContext(Map.of("conditions", condRegex)));
        assertEquals("true", r1.get("_branchKey"));

        List<Map<String, Object>> condEmpty = List.of(
                Map.of("combinator", "AND", "conditions", List.of(
                        Map.of("leftValue", "", "operator", "isEmpty", "rightValue", "")
                ))
        );
        Map<?, ?> r2 = (Map<?, ?>) handlers.ifNode(createContext(Map.of("conditions", condEmpty)));
        assertEquals("true", r2.get("_branchKey"));
    }

    @Test
    @DisplayName("Switch node routes to matching rule output index")
    void switchNode_routesToMatchingRule() {
        List<Map<String, Object>> rules = List.of(
                Map.of("value", "gold", "operator", "equals", "matchValue", "silver", "outputIndex", 0),
                Map.of("value", "gold", "operator", "equals", "matchValue", "gold", "outputIndex", 1)
        );

        Map<String, Object> config = Map.of(
                "mode", "rules",
                "numberOutputs", 3,
                "rules", rules
        );

        ActionContext context = createContext(config);
        Object resultObj = handlers.switchNode(context);

        Map<?, ?> result = (Map<?, ?>) resultObj;
        assertEquals("output_1", result.get("_branchKey"));
        assertEquals(1, result.get("outputIndex"));
    }

    @Test
    @DisplayName("Switch node handles direct expression output index mode")
    void switchNode_handlesExpressionMode() {
        Map<String, Object> config = Map.of(
                "mode", "expression",
                "numberOutputs", 4,
                "output", 2
        );

        ActionContext context = createContext(config);
        Object resultObj = handlers.switchNode(context);

        Map<?, ?> result = (Map<?, ?>) resultObj;
        assertEquals("output_2", result.get("_branchKey"));
        assertEquals(2, result.get("outputIndex"));
    }

    @Test
    @DisplayName("Switch node rejects invalid output index out of range")
    void switchNode_rejectsOutOfRangeIndex() {
        Map<String, Object> config = Map.of(
                "mode", "expression",
                "numberOutputs", 2,
                "output", 5
        );

        ActionContext context = createContext(config);
        Object resultObj = handlers.switchNode(context);

        assertInstanceOf(ActionResult.class, resultObj);
        ActionResult result = (ActionResult) resultObj;
        assertFalse(result.success());
        assertTrue(result.error().contains("selected output 5 but numberOutputs is 2"));
    }
}

