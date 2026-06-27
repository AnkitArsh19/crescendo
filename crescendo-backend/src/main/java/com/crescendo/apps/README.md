# Adding a New App Integration

Each app lives in its own package under `com.crescendo.apps.<appname>`.

## Package Structure

```
apps/
  myapp/
    MyAppApp.java            ← AppDefinition (catalog metadata)
    MyAppHandlers.java       ← Grouped handler (multiple actions per file) ← PREFERRED
    MyAppSomeHandler.java    ← Classic ActionHandler (one action per class) ← also valid
```

---

## Step 1 — Create the AppDefinition

Implement `AppDefinition` and annotate with `@Component`.

```java
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
            List.of(/* triggers */),
            List.of(
                Map.of(
                    "actionKey", "myapp:create",
                    "name", "Create Item",
                    "description", "Creates a new item"
                ),
                Map.of(
                    "actionKey", "myapp:get",
                    "name", "Get Item",
                    "description", "Gets an item by ID"
                )
            )
        );
    }
}
```

---

## Step 2A — Grouped Handler (PREFERRED — n8n style)

Multiple actions in a single `@Component` class. Each method is annotated with `@ActionMapping`.
Credentials are accessed via `context.getCredential("key")`, params via `context.getString("key")`.

```java
@Component
public class MyAppHandlers {

    private String getAuth(ActionContext context) {
        return "Bearer " + context.getCredential("apiKey");
    }

    @ActionMapping(appKey = "myapp", actionKey = "myapp:create")
    public Object create(ActionContext context) throws Exception {
        String name = context.getString("name");
        Map<String, Object> body = Map.of("name", name);
        return RestClient.builder()
                .url("https://api.myapp.com/v1/items")
                .header("Authorization", getAuth(context))
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "myapp", actionKey = "myapp:get")
    public Object get(ActionContext context) throws Exception {
        String id = context.getString("id");
        return RestClient.builder()
                .url("https://api.myapp.com/v1/items/" + id)
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }
}
```

The `ActionHandlerRegistry` automatically scans all `@Component` beans for method-level `@ActionMapping`
and adapts them to the engine's internal action interfaces.

---

## Step 2B — Classic Handler (one class per action)

Still valid for complex actions that need their own class, and many existing apps still use this pattern.

```java
@ActionMapping(appKey = "myapp", actionKey = "myapp:create")
public class MyAppCreateHandler implements ActionHandler {

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds  = context.credentials();
        // ... do work ...
        return ActionResult.success(Map.of("id", "123"));
    }
}
```

---

## ActionContext Convenience Methods

| Method | Description |
|--------|-------------|
| `context.getString("key")` | Get a config parameter as String |
| `context.getString("key", "default")` | Get a config parameter with default |
| `context.getInt("key")` | Get a config parameter as int |
| `context.getInt("key", 0)` | Get a config parameter as int with default |
| `context.getMap("key")` | Get a config parameter as `Map<String, Object>` |
| `context.get("key")` | Get a raw config value as Object |
| `context.getCredential("key")` | Get a credential value (decrypted) |
| `context.configuration()` | Full raw configuration map |
| `context.credentials()` | Full raw credentials map |

---

## RestClient Usage

```java
// GET
RestClient.builder().url("https://...").header("Authorization", token).get().execute();

// POST with body
RestClient.builder().url("https://...").header("Authorization", token).post(bodyMap).execute();

// PUT / PATCH / DELETE
RestClient.builder().url("https://...").put(bodyMap).execute();
RestClient.builder().url("https://...").patch(bodyMap).execute();
RestClient.builder().url("https://...").delete().execute();
```

---

## Key Rules

| Rule | Details |
|------|---------|
| `appKey` | Lowercase, matches pattern `^[a-z][a-z0-9_-]{1,99}$` |
| `appKey` must match | Same value in `AppDefinition.toApp()` and all `@ActionMapping` methods |
| `actionKey` must match | Same value in the action list and `@ActionMapping` |
| Return on failure | `ActionResult.failure("message")` (classic) or throw exception (grouped handler) |
| Return on success | `ActionResult.success(outputMap)` (classic) or return `Map` / `Object` (grouped handler) |
| Credentials | Available via `context.getCredential("key")` or `context.credentials()` |
