# API authentication and scopes

The Public API accepts a Crescendo API key as a Bearer token. API keys are for trusted server-to-server requests; dashboard sessions and browser cookies are not a supported integration interface.

## Create and protect a key

Create a key from **Email → API Keys**. Give it a descriptive name, an expiry, a rate limit, and only the scopes that the application needs. Copy the plaintext value immediately and store it as `CRESCENDO_API_KEY` in your deployment’s secret store.

```bash
export CRESCENDO_API_KEY='re_...'
```

Keys use the `re_` prefix. Treat the full value like a password: never log it, commit it, or deliver it to a browser.

## Send the Bearer token

```http
Authorization: Bearer re_...
```

```javascript
const response = await fetch('https://api.crescendo.run/api/v1/apps', {
  headers: { authorization: `Bearer ${process.env.CRESCENDO_API_KEY}` }
});

if (!response.ok) throw new Error(await response.text());
const apps = await response.json();
```

## Scope names

Scopes use singular resource names. The most common ones are:

| Resource | Read | Write / operation |
| --- | --- | --- |
| Workflows | `workflow:read` | `workflow:write`, `workflow:trigger` |
| Runs | `run:read` | `run:cancel` |
| Connections | `connection:read` | `connection:write` |
| Apps | `app:read` | — |
| Email | — | `email:send` |
| Templates | `template:read` | `template:write` |
| Domains | `domain:read` | `domain:write` |
| Contacts | `contact:read` | `contact:write` |
| Suppressions | `suppression:read` | `suppression:write`, `suppression:import` |
| Webhooks | `webhook:read` | `webhook:write` |
| Custom events | `customevent:read` | `customevent:write` |
| Logs and metrics | `logs:read`, `metrics:read` | — |

When a key is missing a scope, the API returns `403 Forbidden`. A missing, revoked, expired, or malformed key returns an authentication error.

## Rotate safely

Rotate keys on a schedule and immediately when a key may have been exposed. Update the secret in your service first, deploy it, verify a low-risk request, and then revoke the old key. Keep separate keys for development, staging, and production so a test integration cannot affect production resources.
