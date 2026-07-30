# App catalog reference

The app catalog is the configuration contract between Workflow Studio, the AI builder, and the workflow engine. It contains each app’s current actions, triggers, input fields, connection requirements, and resource-backed dropdowns.

## Why this page does not duplicate every app

A handwritten directory of every provider becomes wrong as soon as an action, OAuth scope, or field changes. Crescendo exposes the catalog through the Public API so the dashboard and API clients can use the same current schema.

Use the live [App Catalog API reference](/docs/api/apps) to:

1. List the apps available to your API key.
2. Read the details for one `appKey`.
3. Inspect actions, triggers, field names, types, labels, required flags, and connection schema before creating a workflow programmatically.

## Understand catalog fields

| Field type | What it means |
| --- | --- |
| Text, number, boolean, JSON, array | A value you provide directly or derive from a prior step. |
| Select / dropdown | A fixed set of options defined by the app. |
| Dynamic dropdown | A resource list fetched from the selected connection, such as channels, documents, sheets, databases, workspaces, or playlists. |
| Password | A secret configuration value. Keep it in a restricted connection and never put it in workflow text. |

## Build reliably from the catalog

When generating workflow configuration yourself, use the exact `appKey`, trigger or action key, and field names returned by the catalog. Do not infer them from a provider’s product terminology. For dynamic fields, fetch options after a connection is chosen and send the option’s identifier, not only its display label.

The catalog validates structure; a successful workflow still depends on the target provider accepting the credentials, resource ID, payload, and permissions at execution time.
