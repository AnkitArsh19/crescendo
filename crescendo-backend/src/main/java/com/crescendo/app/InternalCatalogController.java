package com.crescendo.app;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/catalog")
public class InternalCatalogController {

    private final AppService appService;
    private final String catalogVersion;
    private List<Map<String, Object>> minifiedCatalogCache;

    public InternalCatalogController(AppService appService) {
        this.appService = appService;
        // Generate a random UUID on startup. This serves as our "hash".
        // Since the catalog is entirely static (based on compiled AppDefinition classes),
        // it only ever changes when the Java backend is redeployed/restarted!
        this.catalogVersion = UUID.randomUUID().toString();
    }

    @GetMapping("/version")
    public Map<String, String> getVersion() {
        return Map.of("version", catalogVersion);
    }

    @GetMapping
    public Map<String, Object> getCatalog() {
        if (minifiedCatalogCache == null) {
            minifiedCatalogCache = appService.listApps().stream()
                .map(app -> {
                    AppDto.AppDetailResponse detail = appService.getApp(app.appKey());
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("appKey", app.appKey());
                    entry.put("name", app.name());
                    entry.put("description", detail.description());
                    entry.put("category", detail.category());
                    entry.put("authType", detail.authType());
                    entry.put("triggers", enrichedOperations(detail.triggers(), "triggerKey", app.appKey()));
                    entry.put("actions", enrichedOperations(detail.actions(), "actionKey", app.appKey()));
                    return entry;
                })
                .collect(Collectors.toList());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("version", catalogVersion);
        response.put("catalog", minifiedCatalogCache);
        return response;
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    /**
     * Converts a list of raw trigger/action maps into enriched entries for the
     * Python catalog, including full configSchema arrays and shape hints for
     * logic nodes so the AI doesn't guess field structures blind.
     *
     * <p>Each returned entry has the shape:
     * <pre>
     * {
     *   "key":         "gmail:send_email",     // triggerKey or actionKey
     *   "name":        "Send Email",
     *   "configSchema": [
     *     { "key": "to", "label": "To", "type": "text", "required": true },
     *     ...
     *   ]
     * }
     * </pre>
     */
    private List<Map<String, Object>> enrichedOperations(
            List<Map<String, Object>> operations,
            String keyField,
            String appKey) {

        if (operations == null) return List.of();

        return operations.stream().map(op -> {
            String opKey  = String.valueOf(op.getOrDefault(keyField, ""));
            String opName = String.valueOf(op.getOrDefault("name", opKey));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawSchema =
                (List<Map<String, Object>>) op.getOrDefault("configSchema", List.of());

            List<Map<String, Object>> enrichedSchema = injectShapeHints(rawSchema, appKey, opKey);

            Map<String, Object> entry = new HashMap<>();
            entry.put("key",          opKey);
            entry.put("name",         opName);
            entry.put("description",  String.valueOf(op.getOrDefault("description", "")));
            entry.put("configSchema", enrichedSchema);
            return entry;
        }).collect(Collectors.toList());
    }

    /**
     * For logic nodes (logic:if, logic:switch), the configSchema fields that are
     * typed "json" carry no structural information. We inject a concrete
     * {@code shapeHint} example so the AI can see exactly what the JSON must look
     * like at inference time. For all other apps the schema is returned as-is.
     */
    private List<Map<String, Object>> injectShapeHints(
            List<Map<String, Object>> schema,
            String appKey,
            String opKey) {

        if (!"logic".equals(appKey)) return schema;

        return schema.stream().map(field -> {
            String fieldKey = String.valueOf(field.getOrDefault("key", ""));
            String hint = getShapeHint(opKey, fieldKey);
            if (hint == null) return field;

            Map<String, Object> enriched = new HashMap<>(field);
            enriched.put("shapeHint", hint);
            return enriched;
        }).collect(Collectors.toList());
    }

    /**
     * Returns a concrete JSON shape example string for known logic fields, or
     * {@code null} if no hint is needed for this field.
     *
     * <h3>logic:if — conditions</h3>
     * An array of condition groups. Each group has a combinator ("AND"/"OR") and
     * an array of individual condition objects:
     * <pre>
     * [
     *   {
     *     "combinator": "AND",
     *     "conditions": [
     *       { "leftValue": "{{steps.1.subject}}", "operator": "contains", "rightValue": "urgent" },
     *       { "leftValue": "{{steps.1.fromEmail}}", "operator": "equals", "rightValue": "boss@company.com" }
     *     ]
     *   }
     * ]
     * </pre>
     *
     * Supported operators: equals, notEquals, contains, notContains, startsWith,
     * endsWith, isEmpty, isNotEmpty, greaterThan, lessThan, greaterThanOrEqual,
     * lessThanOrEqual, regex.
     *
     * <h3>logic:switch — rules (mode: "rules")</h3>
     * An array of routing rules. Each rule evaluates a value and, if matched,
     * routes to a specific output index:
     * <pre>
     * [
     *   { "value": "{{steps.1.status}}", "operator": "equals", "matchValue": "approved", "outputIndex": 0 },
     *   { "value": "{{steps.1.status}}", "operator": "equals", "matchValue": "rejected", "outputIndex": 1 }
     * ]
     * </pre>
     * When mode="expression", set the top-level "output" field to the desired
     * zero-based output index directly (no rules array needed).
     *
     * <h3>logic:if and logic:switch — options</h3>
     * Optional behaviour overrides:
     * <pre>{ "fallbackOutput": 0, "renameBranchOutputs": false }</pre>
     */
    private String getShapeHint(String opKey, String fieldKey) {
        return switch (opKey + ":" + fieldKey) {
            case "logic:if:conditions" ->
                """
                [
                  {
                    "combinator": "AND",
                    "conditions": [
                      {
                        "leftValue": "{{steps.1.fieldName}}",
                        "operator": "contains",
                        "rightValue": "urgent"
                      }
                    ]
                  }
                ]
                Supported operators: equals, notEquals, contains, notContains,
                startsWith, endsWith, isEmpty, isNotEmpty, greaterThan, lessThan,
                greaterThanOrEqual, lessThanOrEqual, regex""";

            case "logic:switch:rules" ->
                """
                Used when mode="rules" (default). Array of routing rules:
                [
                  {
                    "value": "{{steps.1.fieldName}}",
                    "operator": "equals",
                    "matchValue": "approved",
                    "outputIndex": 0
                  },
                  {
                    "value": "{{steps.1.fieldName}}",
                    "operator": "equals",
                    "matchValue": "rejected",
                    "outputIndex": 1
                  }
                ]
                When mode="expression", leave rules empty and set "output" to the
                desired zero-based output index number directly.""";

            case "logic:if:options", "logic:switch:options" ->
                """
                { "fallbackOutput": 0, "renameBranchOutputs": false }""";

            default -> null;
        };
    }
}
