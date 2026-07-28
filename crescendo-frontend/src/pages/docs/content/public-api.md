# Developer API Reference

Crescendo operates as both an interactive visual automation suite and a scalable headless processing engine. Our comprehensive REST Public API enables enterprise developers to manage account resources, orchestrate execution instances, and query step telemetry programmatically without accessing the visual dashboard UI.

## Authentication & Authorization

All Developer API endpoints enforce strict token-based authorization and demand a valid Bearer token within every HTTP request header:
```bash
Authorization: Bearer <YOUR_API_KEY>
```

You can provision Secret API Keys within your user dashboard under **Settings > Developer API**.

### Role-Based API Scopes
When registering a new API secret key, you must specify explicit role-based permission boundaries to enforce secure access control:
* `workflows:read` : Grant read-only access to workflow configurations and canvas node structures.
* `workflows:write` : Permit programmatic creation, editing, and architectural modification of workflows.
* `runs:read` : Authorize access to execution logs, latency statistics, and diagnostic run traces.
* `runs:execute` : Enable remote server systems to initiate workflow execution instances programmatically.

> [!CAUTION]
> Protect your secret API keys with maximum security. Never expose credentials inside client-side browser frameworks (such as frontend React application bundles) or public version control repositories. Always route automated programmatic calls through secure corporate backend servers.

## Triggering Workflow Executions Programmatically

To trigger an automated workflow via HTTP request invocation, the target workflow must exist in an **Active** state and contain an initialized **Webhook / API Trigger** starting node.

### `POST /api/v1/public/workflows/{workflowId}/execute`

**Request Headers:**
```http
Authorization: Bearer re_1234567890abcdef
Content-Type: application/json
Idempotency-Key: req_unique_id_998877
```

**Request Body (JSON Payload):**
```json
{
  "triggerData": {
    "customerId": "cus_98765",
    "event": "subscription_upgraded",
    "tier": "enterprise_plus",
    "timestamp": 1785000000
  }
}
```

**Response (202 Accepted):**
```json
{
  "runId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "message": "Workflow execution queued successfully in event streaming architecture."
}
```

Because Crescendo utilizes a high-performance event-driven, stream-based distributed processing engine, API executions function asynchronously. The API returns an instantaneous acknowledgment containing a unique `runId`, queuing the requested background tasks without keeping your HTTP socket blocked during lengthy third-party integration sequences.

## Polling Execution Run Status

To retrieve execution results or inspect individual node step completion timestamps, query the run tracking endpoint utilizing your received `runId`.

### `GET /api/v1/public/runs/{runId}`

**Response (200 OK):**
```json
{
  "runId": "550e8400-e29b-41d4-a716-446655440000",
  "workflowId": "wf_88990011",
  "status": "SUCCESS",
  "stepsExecuted": 4,
  "startedAt": "2026-07-28T10:00:00.104Z",
  "completedAt": "2026-07-28T10:00:01.420Z",
  "stepResults": [
    {
      "stepOrder": 1,
      "appKey": "slack",
      "actionKey": "post_message",
      "status": "SUCCESS",
      "latencyMs": 142
    }
  ]
}
```

## Official SDKs & Libraries

To simplify corporate backend engineering integrations, we provide officially supported software development kits featuring automatic token management and typed schema interfaces:
* **Node.js & TypeScript:** Install via npm package manager (`npm install @crescendo/node`).
* **Python (3.10+):** Install via Python Package Index (`pip install crescendo-sdk`).
* **Java & Spring Boot:** Reference our standard OpenAPI v3 JSON specification directly to auto-generate client communication beans using standard Swagger or OpenFeign REST code generator plugins.
