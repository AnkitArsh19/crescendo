# Node.js and TypeScript SDK

`@crescendo/email` is the hand-written JavaScript client for Crescendo’s public API. It requires Node.js 18 or later because it uses the built-in `fetch` API.

## Install and configure

```bash
npm install @crescendo/email
export CRESCENDO_API_KEY='re_...'
```

```javascript
const { crescendo } = require('@crescendo/email');

const client = crescendo({
  apiKey: process.env.CRESCENDO_API_KEY
});
```

For local or self-hosted development, set `CRESCENDO_BASE_URL` before starting your application. The default is `https://api.crescendo.run`.

## Send an email

The sender must belong to a verified Crescendo domain. Use `TRANSACTIONAL` for product and account messages; make sure recipients have the necessary consent for marketing mail.

```javascript
const result = await client.emails.send({
  from: 'updates@your-verified-domain.example',
  to: 'recipient@example.com',
  subject: 'Your order is confirmed',
  htmlBody: '<p>Thanks for your order.</p>',
  textBody: 'Thanks for your order.',
  emailType: 'TRANSACTIONAL',
  tags: { source: 'checkout' }
});

console.log(result);
```

## Use a template

Create and publish a template before sending with it. Template variables are passed as a plain object.

```javascript
const template = await client.templates.create({
  name: 'Welcome',
  subject: 'Welcome, {{FIRST_NAME}}',
  htmlBody: '<h1>Welcome, {{FIRST_NAME}}</h1>',
  variables: [{ name: 'FIRST_NAME', type: 'STRING' }]
});

await client.templates.publish(template.id);

await client.emails.sendTemplated({
  from: 'updates@your-verified-domain.example',
  to: 'recipient@example.com',
  templateId: template.id,
  variables: { FIRST_NAME: 'Asha' }
});
```

## Available client modules

The client exposes `emails`, `templates`, `contacts`, `suppressions`, `domains`, `metrics`, `webhooks`, `workflows`, `connections`, `apps`, and `runs`.

Use the module that matches the public resource. For example, `client.workflows.trigger(id, payload)` calls the workflow trigger endpoint; `client.runs.get(workflowId, runId)` reads an individual run. The SDK does not bypass permissions: the API key must still have the endpoint’s required scope.

## Handle errors

The client throws `CrescendoError` for non-success API responses. It contains `statusCode` and the decoded `response` body.

```javascript
const { crescendo, CrescendoError } = require('@crescendo/email');

try {
  await client.domains.add('mail.example.com');
} catch (error) {
  if (error instanceof CrescendoError) {
    console.error(error.statusCode, error.response);
  } else {
    throw error;
  }
}
```

For endpoint fields and response shapes, use the live [OpenAPI reference](/docs/api/emails). It is the contract the SDK is designed to call.
