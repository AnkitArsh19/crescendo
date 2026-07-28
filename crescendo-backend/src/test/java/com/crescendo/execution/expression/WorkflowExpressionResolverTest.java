package com.crescendo.execution.expression;

import com.crescendo.steps.steps_command.Steps_command;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowExpressionResolverTest {

    private final WorkflowExpressionResolver resolver = new WorkflowExpressionResolver();

    @Test
    @DisplayName("Resolves nested step output references without losing native types")
    void resolvesNestedReferences_withoutLosingNativeTypes() {
        UUID sourceId = UUID.randomUUID();
        Map<UUID, Map<String, Object>> outputs = Map.of(sourceId, Map.of(
                "payload", Map.of("priority", 7, "tags", List.of("urgent", "customer"))));

        Map<String, Object> config = Map.of(
                "conditions", List.of(Map.of("leftValue", "{{steps." + sourceId + ".payload.priority}}")),
                "tag", Map.of("$ref", Map.of("step", sourceId.toString(), "path", "payload.tags.0")),
                "summary", "Priority {{steps." + sourceId + ".payload.priority}}");

        Map<String, Object> result = resolver.resolveConfiguration(config, outputs, Map.of());

        List<?> conditions = (List<?>) result.get("conditions");
        assertEquals(7, ((Map<?, ?>) conditions.getFirst()).get("leftValue"));
        assertInstanceOf(Integer.class, ((Map<?, ?>) conditions.getFirst()).get("leftValue"));
        assertEquals("urgent", result.get("tag"));
        assertEquals("Priority 7", result.get("summary"));
    }

    @Test
    @DisplayName("Resolves legacy order-based step references deterministically")
    void resolvesLegacyOrderReferencesDeterministically() {
        UUID sourceId = UUID.randomUUID();
        Steps_command source = new Steps_command();
        source.setOrder(BigDecimal.valueOf(3));

        Map<String, Object> result = resolver.resolveConfiguration(
                Map.of("value", "{{steps.3.status}}"),
                Map.of(sourceId, Map.of("status", "approved")),
                Map.of(sourceId, source));

        assertEquals("approved", result.get("value"));
    }

    @Test
    @DisplayName("Handles missing keys and out-of-bounds array indices gracefully")
    void handlesMissingKeysAndOutOfBoundsIndicesGracefully() {
        UUID sourceId = UUID.randomUUID();
        Map<UUID, Map<String, Object>> outputs = Map.of(sourceId, Map.of("items", List.of("first")));

        Map<String, Object> config = Map.of(
                "missingKey", "{{steps." + sourceId + ".nonExistentField}}",
                "outOfBounds", "{{steps." + sourceId + ".items.99}}"
        );

        Map<String, Object> result = resolver.resolveConfiguration(config, outputs, Map.of());

        assertNull(result.get("missingKey"));
        assertNull(result.get("outOfBounds"));
    }
}
