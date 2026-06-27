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
        return new App("crescendo-log", "Internal Debug Log", """
                The Internal Debug Log is a special-purpose app used strictly by workflow administrators and developers to print diagnostic information directly to the backend server's console.

                **What you can do with Debug Log in Crescendo:**
                - Print the raw JSON payload of an incoming webhook to understand its structure before building out the rest of the workflow
                - Output intermediate variable values midway through a complex workflow to track down logical errors
                - Log the specific execution paths taken by conditional logic gates during testing

                **Actions available:**
                - Print to Log — takes any input data and safely prints it to the Crescendo application server logs (stdout).

                **Who should use this:** Crescendo platform administrators and developers actively debugging complex automation flows.

                **Authentication:** None required.
                """,
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
