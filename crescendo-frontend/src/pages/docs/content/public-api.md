# Public API overview

The Crescendo Public API lets a trusted server manage workflows, connections, email, templates, domains, contacts, app metadata, webhooks, and workflow runs. Its canonical base URL is:

```text
https://api.crescendo.run
```

The API paths begin with `/api/v1`. For example, workflows are available at `/api/v1/workflows` — not at `/api/v1/public/workflows`.

## Before you call the API

1. In Crescendo, open **Email → API Keys** and create an API key.
2. Give the key only the scopes needed by your service.
3. Store the plaintext key in a server-side secret manager or environment variable. It is shown only once.
4. Send it in the `Authorization` header from your backend. Do not put it in browser code, a mobile app, or a public repository.

```bash
curl --request GET 'https://api.crescendo.run/api/v1/workflows' \
  --header "Authorization: Bearer $CRESCENDO_API_KEY"
```

## Core API areas

| Area | Use it for | Typical scope |
| --- | --- | --- |
| Workflows | Create, read, update, activate, deactivate, and trigger workflows | `workflow:read`, `workflow:write`, `workflow:trigger` |
| Runs | List, inspect, and cancel workflow runs | `run:read`, `run:cancel` |
| Connections and apps | Configure provider accounts and read the catalog schema | `connection:read`, `connection:write`, `app:read` |
| Emails | Send transactional email and inspect message logs | `email:send`, `logs:read` |
| Templates | Manage reusable email templates | `template:read`, `template:write` |
| Domains, contacts, suppressions | Manage sending infrastructure and audiences | the matching `domain:*`, `contact:*`, and `suppression:*` scopes |
| Outbound webhooks and custom events | Subscribe to outbound events or define and fire custom events | `webhook:*`, `customevent:*` |

The exact endpoint, request schema, response schema, and required scope are shown in the live [OpenAPI reference](/docs/api/workflows). That reference is generated from the deployed public API contract, so it is more precise than copied examples.

## A safe workflow trigger

An active workflow can be triggered programmatically. The request body is the trigger data made available to the workflow; pass the object directly, rather than wrapping it in a second `triggerData` field.

```bash
curl --request POST 'https://api.crescendo.run/api/v1/workflows/<workflowId>/trigger' \
  --header "Authorization: Bearer $CRESCENDO_API_KEY" \
  --header 'Content-Type: application/json' \
  --header 'Idempotency-Key: order-1234-paid-v1' \
  --data '{"orderId":"1234","event":"payment.succeeded"}'
```

The API accepts the request and runs the workflow asynchronously. Save the returned run identifier and use the workflow-runs endpoints to inspect the result.

## SDKs and HTTP clients

Use the Node.js or Python SDK for a hand-written client experience. The Java, Go, PHP, Ruby, Rust, and .NET clients are generated from the same OpenAPI contract. If you use another language, the live reference has request examples for common HTTP clients.

- [Node.js / TypeScript SDK](/docs/sdk-node)
- [Python SDK](/docs/sdk-python)
- [Generated SDKs and CLI](/docs/sdk-multi-language)
- [Authentication and scopes](/docs/authentication)
- [Idempotency and errors](/docs/api-governance)
