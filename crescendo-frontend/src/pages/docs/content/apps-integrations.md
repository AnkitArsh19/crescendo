# Connections and integrations

An app becomes usable in a workflow when you create a connection for it. The connection stores the account authorization that an action or trigger uses at run time.

## Connect an app

1. Open **Connections**.
2. Select an app and choose the available authentication method.
3. For OAuth, complete the provider consent screen and return to Crescendo. For API keys or tokens, enter a credential created in the provider’s own dashboard.
4. Give the connection a clear label when you have more than one account.
5. In Workflow Studio, select that connection on each app node that needs it.

Connection availability is account-specific. A Slack channel, Google Sheet, Notion database, or Spotify playlist must be selected from the resources returned for the chosen connection. If you switch connections, revisit those fields.

## OAuth and API keys

- **OAuth** lets the provider grant Crescendo scoped access without you sharing a password. The provider decides the consent scopes and refresh behavior.
- **API key / token** connections use a credential you generate in the provider’s dashboard. Limit its permissions and rotate it if it is exposed.
- **Custom OAuth apps** are for organizations that need their own provider client ID and redirect configuration. Follow the provider-specific settings shown by the connection flow; redirect URIs and required scopes differ by provider.

> [!CAUTION]
> Do not paste a personal password into a connection form. Use OAuth, an API key, an access token, or another credential type explicitly supported by that app.

## The app catalog is the source of truth

The catalog changes as integrations evolve. Each app exposes its currently supported triggers, actions, fields, connection requirements, and dynamic resource types. For programmatic access, call the App Catalog API with `app:read`; the live [App Catalog reference](/docs/api/apps) shows that schema.

The catalog proves what Crescendo can configure. Provider permissions, selected resources, network availability, and account-specific policy are still checked during execution.

## Use HTTP when an app is not available

Use the HTTP action for a documented third-party or internal API when no native action fits. Keep credentials in a connection or secure service, use a stable request body, and inspect the target API’s response before feeding it to another step. Do not use an HTTP action to send secrets to an untrusted URL.
