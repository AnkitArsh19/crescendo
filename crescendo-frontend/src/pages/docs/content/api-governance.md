# API Governance

The Crescendo Public API enforces strict governance rules to protect both our platform and your application from errors. 

## Idempotency

Network errors happen. Sometimes, you may issue a successful `POST` request to trigger a workflow, but the connection drops before you receive the `202 Accepted` response. To prevent accidentally triggering a workflow twice during a retry loop, Crescendo supports idempotency keys.

Include an `Idempotency-Key` header on any state-mutating `POST` or `PATCH` request.

```bash language-bash
curl -X POST "https://api.crescendo.run/api/v1/workflows/123/trigger" \
  -H "Authorization: Bearer re_123" \
  -H "Idempotency-Key: my_unique_key_123" \
  -d '{"foo": "bar"}'
```

> [!IMPORTANT]
> **Strict Conflict Checking (Stripe-style)**
> We cache successful responses by your `Idempotency-Key` for 24 hours. However, we also hash your request payload. If you reuse an `Idempotency-Key` but send a **different payload**, we will explicitly return a `409 Conflict`. We do not silently serve cached responses for mismatched payloads.

## Standardized Errors

Crescendo API endpoints do not leak internal stack traces. All client errors (`4xx`) and server errors (`5xx`) follow a strictly typed JSON structure.

```json language-json
{
  "type": "https://crescendo.com/errors/conflict",
  "title": "Idempotency Conflict",
  "status": 409,
  "detail": "Duplicate Idempotency-Key for this workflow trigger."
}
```

## Opaque Cursor Pagination

When retrieving large collections (like Domains or Audiences), the API wraps the data in an explicit pagination envelope.

```json language-json
{
  "data": [
    { "id": "uuid-1", "name": "example.com" },
    { "id": "uuid-2", "name": "demo.com" }
  ],
  "has_more": true,
  "next_cursor": "T2Zmc2V0OjEwMA=="
}
```

To fetch the next page, pass the `next_cursor` back to the endpoint using the `after` query parameter:

```bash language-bash
curl "https://api.crescendo.run/api/v1/domains?limit=100&after=T2Zmc2V0OjEwMA=="
```

> [!NOTE]
> Do not attempt to decode or manipulate the `next_cursor`. Its structure is deliberately opaque and subject to change as our database partitioning strategies evolve.
