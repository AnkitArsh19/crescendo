# Connecting apps: OAuth and API credentials

Each integration declares the credential types it supports. When you add a connection, Crescendo presents the configuration required for that app; do not assume that an app supports every authentication method.

## Use OAuth when it is available

OAuth delegates scoped access to a provider without sharing the account password.

1. Open **Connections** and choose the app.
2. Select OAuth and continue to the provider’s consent page.
3. Review the provider’s account and requested permissions.
4. Approve only if the account and scopes are appropriate, then return to Crescendo.

If the connection later fails, reconnect it from Crescendo. The provider may have revoked access, changed its consent policy, or required a new authorization.

## Use an API key or token carefully

For services that use API keys, personal access tokens, or bearer tokens:

1. Create a dedicated credential in the provider’s developer console.
2. Give it only the permissions the workflow requires.
3. Add it through the app’s connection form.
4. Label the connection clearly and rotate or delete it when no longer needed.

Do not reuse a personal credential across unrelated environments. Use separate test and production accounts where the provider supports them.

## Custom OAuth applications

Some organizations prefer to bring their own OAuth client ID and client secret. Register the client with the provider, then enter the exact redirect URI and requested scopes shown in Crescendo’s connection setup. Provider-specific redirect paths and scopes are not interchangeable, so copy them from the selected app rather than from another provider’s documentation.

## What Crescendo stores

Crescendo uses the connection to make the calls configured in a workflow. Credentials are not exposed again through the normal workspace interface. Still, treat access to the Crescendo workspace as sensitive: anyone who can edit a workflow or connection may be able to cause the connected account to act.
