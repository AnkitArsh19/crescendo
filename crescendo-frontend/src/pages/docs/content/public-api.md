# Developer API

Crescendo isn't just a visual builder; it's a powerful headless automation engine. The Public API allows developers to trigger workflows, monitor execution logs, and manage account resources programmatically.

## Authentication

All API endpoints are protected and require a Bearer token.

```bash
Authorization: Bearer <YOUR_API_KEY>
```

You can generate API Keys in your **Settings → Developer API**. 

### API Scopes

When creating an API key, you must assign specific scopes. Crescendo enforces strict granular access control:
- `workflows:read` — View workflow configurations.
- `workflows:write` — Create or modify workflows.
- `runs:read` — View execution logs and step statistics.
- `runs:execute` — Trigger workflows programmatically.

> [!CAUTION]
> Never expose your API keys in client-side code (like frontend React applications). Always proxy requests through your own secure backend.

## Triggering a Workflow

To trigger a workflow programmatically, the workflow must be **Active** and configured with a **Webhook/API Trigger**.

### `POST /api/v1/public/workflows/{workflowId}/execute`

**Request Body (JSON):**
```json
{
  "triggerData": {
    "customerId": "cus_12345",
    "event": "subscription_created",
    "plan": "enterprise"
  }
}
```

**Response (202 Accepted):**
```json
{
  "runId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING"
}
```

Because Crescendo uses an event-driven, stream-based architecture, API executions are asynchronous. The API acknowledges the request instantly and queues the execution, returning a `runId` you can use to poll for status.

## Retrieving Run Status

### `GET /api/v1/public/runs/{runId}`

**Response (200 OK):**
```json
{
  "runId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "SUCCESS",
  "stepsExecuted": 4,
  "startedAt": "2026-06-30T10:00:00Z",
  "completedAt": "2026-06-30T10:00:02Z"
}
```

## SDKs and Libraries
Currently, we offer official SDKs for:
- Node.js (`npm install @crescendo/node`)
- Python (`pip install crescendo-sdk`)

If you are using a different language, you can interact directly with our REST APIs over HTTPS.
