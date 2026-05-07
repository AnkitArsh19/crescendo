# Adding a New App Integration

Each app lives in its own package under `com.crescendo.apps.<appname>`.

## Package Structure

```
apps/
  myapp/
    MyAppApp.java            ← AppDefinition (catalog metadata)
    MyAppSomeHandler.java    ← ActionHandler (one per action)
    MyAppOtherHandler.java   ← (optional) additional actions
```

## Step 1 — Create the AppDefinition

Implement `AppDefinition` and annotate with `@Component`.  
The `toApp()` method returns the `App` entity that gets seeded into the catalog.

```java
package com.crescendo.apps.myapp;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MyAppApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
            "myapp",                          // appKey (lowercase, a-z0-9_-)
            "My App",                         // display name
            "Short description of the app",   // description
            "/icons/myapp.svg",               // icon path
            AuthType.APIKEY,                  // NONE | APIKEY | OAUTH2
            List.of(/* triggers */),          // trigger definitions (can be empty)
            List.of(                          // action definitions
                Map.of(
                    "actionKey", "do-thing",
                    "name", "Do a Thing",
                    "description", "Performs some action",
                    "configSchema", Map.of(
                        "param1", "string (required) — description",
                        "param2", "integer — optional param"
                    )
                )
            )
        );
    }
}
```

## Step 2 — Create ActionHandler(s)

One handler per action. Use `@ActionMapping` (which includes `@Component`).

```java
package com.crescendo.apps.myapp;

import com.crescendo.execution.action.*;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@ActionMapping(appKey = "myapp", actionKey = "do-thing")
public class MyAppDoThingHandler implements ActionHandler {

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds  = context.credentials();

        // Use RestClient for HTTP calls
        String response = RestClient.create()
            .post()
            .uri("https://api.myapp.com/v1/things")
            .header("Authorization", "Bearer " + creds.get("apiKey"))
            .body(Map.of("param1", config.get("param1")))
            .retrieve()
            .body(String.class);

        Map<String, Object> output = new HashMap<>();
        output.put("result", response);
        return ActionResult.success(output);
    }
}
```

## That's It

- **No registration needed** — Spring auto-discovers `@Component` beans
- **No DataSeeder changes** — `AppDefinition` beans are injected automatically
- **No registry changes** — `ActionHandlerRegistry` finds handlers via `@ActionMapping`

## Key Rules

| Rule | Details |
|------|---------|
| `appKey` | Lowercase, matches pattern `^[a-z][a-z0-9_-]{1,99}$` |
| `appKey` must match | Same value in `AppDefinition.toApp()` and `@ActionMapping` |
| `actionKey` must match | Same value in the action list and `@ActionMapping` |
| Return on failure | `ActionResult.failure("message")` |
| Return on success | `ActionResult.success(outputMap)` |
| Credentials | Available via `context.credentials()` — populated from the user's Connection |
