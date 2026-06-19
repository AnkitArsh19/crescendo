package com.crescendo.apps.logic;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class LogicApp implements AppDefinition {
    @Override
    public App toApp() {
        List<Map<String, Object>> conditionFields = List.of(
                Map.of("key", "field", "label", "Input Field", "type", "text", "required", false,
                        "placeholder", "status", "helpText", "Dot path in the incoming data. Leave blank to compare the whole input."),
                Map.of("key", "left", "label", "Left Value", "type", "text", "required", false,
                        "placeholder", "{{steps.1.status}}", "helpText", "Explicit value to compare. Takes precedence over Input Field."),
                Map.of("key", "operator", "label", "Operator", "type", "select", "required", true,
                        "options", List.of(
                                Map.of("value", "equals", "label", "Equals"),
                                Map.of("value", "not_equals", "label", "Does Not Equal"),
                                Map.of("value", "contains", "label", "Contains"),
                                Map.of("value", "greater_than", "label", "Greater Than"),
                                Map.of("value", "less_than", "label", "Less Than"),
                                Map.of("value", "exists", "label", "Exists"),
                                Map.of("value", "is_empty", "label", "Is Empty"),
                                Map.of("value", "regex", "label", "Regex")
                        )),
                Map.of("key", "right", "label", "Right Value", "type", "text", "required", false,
                        "placeholder", "approved", "helpText", "Comparison value")
        );

        return new App("logic", "Logic", "Branch, filter, batch, and merge workflow data",
                "/icons/logic.svg", AuthType.NONE,
                List.of(),
                List.of(
                        Map.of("actionKey", "if", "name", "If / Condition",
                                "description", "Route to true or false branch based on a condition",
                                "configSchema", conditionFields),
                        Map.of("actionKey", "switch", "name", "Switch",
                                "description", "Route to one of many branches",
                                "configSchema", List.of(
                                        Map.of("key", "field", "label", "Input Field", "type", "text", "required", false,
                                                "placeholder", "status", "helpText", "Dot path to compare"),
                                        Map.of("key", "value", "label", "Value", "type", "text", "required", false,
                                                "placeholder", "{{steps.1.status}}", "helpText", "Explicit value. Takes precedence over Input Field."),
                                        Map.of("key", "cases", "label", "Cases", "type", "json", "required", true,
                                                "placeholder", "[{\"value\":\"paid\",\"branchKey\":\"paid\"}]", "helpText", "Array of case objects with value, branchKey, and optional operator."),
                                        Map.of("key", "defaultBranchKey", "label", "Default Branch", "type", "text", "required", false,
                                                "placeholder", "default", "helpText", "Branch used when no case matches")
                                )),
                        Map.of("actionKey", "filter", "name", "Filter",
                                "description", "Keep list items that match a condition",
                                "configSchema", conditionFields),
                        Map.of("actionKey", "merge", "name", "Merge",
                                "description", "Merge current input with configured data",
                                "configSchema", List.of(
                                        Map.of("key", "mode", "label", "Mode", "type", "select", "required", false,
                                                "options", List.of(
                                                        Map.of("value", "merge", "label", "Merge Objects"),
                                                        Map.of("value", "append", "label", "Append Lists")
                                                )),
                                        Map.of("key", "data", "label", "Data", "type", "json", "required", false,
                                                "placeholder", "{\"key\":\"value\"}", "helpText", "Object or array to merge with current input")
                                )),
                        Map.of("actionKey", "split-in-batches", "name", "Split In Batches",
                                "description", "Return one batch from a list",
                                "configSchema", List.of(
                                        Map.of("key", "itemsPath", "label", "Items Path", "type", "text", "required", false,
                                                "placeholder", "items", "helpText", "Dot path to an array in the input. Leave blank if input itself is an array."),
                                        Map.of("key", "batchSize", "label", "Batch Size", "type", "number", "required", false,
                                                "placeholder", "10", "helpText", "Items per batch"),
                                        Map.of("key", "batchIndex", "label", "Batch Index", "type", "number", "required", false,
                                                "placeholder", "0", "helpText", "Zero-based batch index")
                                ))
                )
        ).credentialSchema(List.of()).category("core").helpUrl("");
    }
}
