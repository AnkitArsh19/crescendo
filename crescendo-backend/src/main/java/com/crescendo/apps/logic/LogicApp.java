package com.crescendo.apps.logic;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for Logic.
 */
@Component
public class LogicApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "logic",
                "Logic",
                """
                Core logic nodes like If, Switch, etc.
                
                This integration provides operations for:
                - **If**: Route items to different branches (true/false)
                - **Switch**: Route items depending on defined expression or rules
                """,
                "/icons/logic.svg", // Generic icon
                AuthType.NONE,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "logic:if",
                                "name", "If",
                                "description", "Route items to different branches (true/false)",
                                "configSchema", List.of(
                                        Map.of("key", "conditions", "label", "Conditions", "type", "json", "required", true),
                                        Map.of("key", "options", "label", "Options", "type", "json")
                                )
                        ),
                        Map.of(
                                "actionKey", "logic:switch",
                                "name", "Switch",
                                "description", "Route items depending on defined expression or rules",
                                "configSchema", List.of(
                                        Map.of("key", "mode", "label", "Mode", "type", "select", "options", List.of("rules", "expression"), "default", "rules"),
                                        Map.of("key", "numberOutputs", "label", "Number of Outputs", "type", "number", "default", 4),
                                        Map.of("key", "output", "label", "Output Index", "type", "number"),
                                        Map.of("key", "rules", "label", "Routing Rules", "type", "json"),
                                        Map.of("key", "options", "label", "Options", "type", "json")
                                )
                        ),
                        Map.of(
                                "actionKey", "logic:merge",
                                "name", "Merge",
                                "description", "Combine branches after an If or Switch",
                                "configSchema", List.of(
                                        Map.of("key", "mode", "label", "Mode", "type", "select", "options", List.of("all", "any"), "default", "all")
                                )
                        )
                )
        ).credentialSchema(List.of()).category("logic-and-flow");
    }
}
