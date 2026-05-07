package com.crescendo.apps.log;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class LogApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("crescendo-log", "Internal Debug Log",
                "Internal utility that prints step data to the server log. Not visible to end users.",
                "/icons/log.svg", AuthType.NONE,
                List.of(),
                List.of(Map.of(
                    "actionKey", "print",
                    "name", "Print to Log",
                    "description", "Log the step's input data to the server console"
                ))
        )
        .credentialSchema(List.of())
        .category("internal")
        .helpUrl("")
        .internal(true);
    }
}
