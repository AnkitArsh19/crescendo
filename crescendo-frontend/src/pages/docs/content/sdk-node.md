# Node.js & TypeScript SDK Reference (`@crescendo/email`)

The official `@crescendo/email` SDK provides a hand-crafted, fully typed interface for interacting with the Crescendo REST API in Node.js and TypeScript environments.

## Installation

Install the package via npm, yarn, or pnpm:

```bash language-bash
npm install @crescendo/email
```

## Initialization

Import the factory function `crescendo` and initialize the client with your secret API key (starting with `cm_sk_` or `re_`).

```javascript language-javascript
import { crescendo } from '@crescendo/email';

const client = crescendo({
  apiKey: process.env.CRESCENDO_API_KEY || 'cm_sk_live_1234567890abcdef'
});
```

You can also specify a custom host endpoint by setting the `CRESCENDO_BASE_URL` environment variable:
```bash language-bash
export CRESCENDO_BASE_URL="https://custom.api.crescendo.run"
```

## Error Handling

All API errors throw an instance of `CrescendoError`, containing the HTTP status code and raw JSON response payload:

```javascript language-javascript
import { crescendo, CrescendoError } from '@crescendo/email';

const client = crescendo({ apiKey: 'cm_sk_invalid' });

try {
  await client.emails.send({
    from: 'hello@example.com',
    to: 'user@example.com',
    subject: 'Welcome',
    htmlBody: '<p>Hello</p>'
  });
} catch (error) {
  if (error instanceof CrescendoError) {
    console.error(`Crescendo API Error [${error.statusCode}]:`, error.message);
    console.error('Response Payload:', error.response);
  } else {
    console.error('Network or Execution Error:', error);
  }
}
```

---

## Service Modules & Methods

The client instance exposes 11 dedicated service modules:

### 1. `client.emails`

#### `emails.send(params)`
Dispatches a single transactional or marketing email message.

```javascript language-javascript
const response = await client.emails.send({
  from: 'notifications@verified-domain.com',
  to: 'customer@example.com',
  subject: 'Order Confirmation #1094',
  htmlBody: '<h1>Thank you for your order!</h1>',
  textBody: 'Thank you for your order!',
  emailType: 'TRANSACTIONAL', // 'TRANSACTIONAL' | 'MARKETING'
  tags: { campaign: 'onboarding_2026', source: 'checkout' }
});
// Response: { id: "msg_998877", status: "SENT", createdAt: "2026-07-28T10:00:00Z" }
```

#### `emails.sendTemplated(params)`
Sends an email using a snapshot of a published template ID.

```javascript language-javascript
const response = await client.emails.sendTemplated({
  from: 'news@verified-domain.com',
  to: 'subscriber@example.com',
  templateId: '3fa85f64-5717-4562-b3fc-2c963f66afa6',
  variables: {
    FIRST_NAME: 'Alexander',
    ACCOUNT_TIER: 'Enterprise'
  },
  emailType: 'MARKETING'
});
```

#### `emails.sendBatch(params)`
Transmits identical email structures to multiple recipients in a single HTTP invocation (up to 100 recipients per batch).

```javascript language-javascript
const response = await client.emails.sendBatch({
  from: 'updates@verified-domain.com',
  subject: 'System Maintenance Window Notice',
  htmlBody: '<p>Our maintenance begins tonight at 02:00 UTC.</p>',
  recipients: [
    'user1@domain.com',
    { to: 'user2@domain.com', customTag: 'vip' }
  ]
});
```

#### `emails.get(emailId)`
Retrieves real-time delivery status for a specific message ID.

```javascript language-javascript
const status = await client.emails.get('msg_998877');
console.log(status); // { id: "msg_998877", status: "DELIVERED", deliveredAt: "..." }
```

#### `emails.list(filters)`
Lists historical email logs with status and tag filtering options.

```javascript language-javascript
const logs = await client.emails.list({
  status: 'DELIVERED', // 'SENT' | 'DELIVERED' | 'FAILED' | 'BOUNCED'
  limit: 25,
  after: '2026-07-01T00:00:00Z'
});
```

---

### 2. `client.templates`

#### `templates.create(params)`
Creates a new draft template.

```javascript language-javascript
const template = await client.templates.create({
  name: 'Monthly Newsletter Layout',
  subject: 'Your Monthly Report for {{MONTH}}',
  htmlBody: '<div>Hello {{FIRST_NAME}}, here is your summary...</div>',
  textBody: 'Hello {{FIRST_NAME}}, here is your summary...',
  variables: [
    { name: 'FIRST_NAME', type: 'STRING', fallbackValue: 'Valued Customer' },
    { name: 'MONTH', type: 'STRING' }
  ]
});
```

#### `templates.publish(id)`
Validates template variable placeholders and freezes a immutable published version snapshot.

```javascript language-javascript
const published = await client.templates.publish(template.id);
```

#### `templates.testSend(id, params)`
Dispatches a test simulation email using draft content without impacting production quotas.

```javascript language-javascript
await client.templates.testSend(template.id, {
  toAddress: 'internal-qa@company.com',
  variables: { FIRST_NAME: 'Test User', MONTH: 'July' }
});
```

---

### 3. `client.contacts` (Audiences)

#### `contacts.upsert(params)`
Creates or updates a subscriber record using their email address as the unique identifier key.

```javascript language-javascript
const contact = await client.contacts.upsert({
  email: 'john.doe@company.com',
  firstName: 'John',
  lastName: 'Doe',
  customProperties: {
    plan: 'enterprise',
    sign_up_source: 'web_form'
  }
});
```

#### `contacts.setProperty(email, property, value)`
Sets a custom property value on a contact, driving automated dynamic audience segmentation rules.

```javascript language-javascript
await client.contacts.setProperty('john.doe@company.com', 'account_status', 'active');
```

---

### 4. `client.workflows`

#### `workflows.trigger(id, params)`
Programmatically triggers execution of an active workflow.

```javascript language-javascript
const result = await client.workflows.trigger('wf_998877', {
  triggerData: {
    event: 'user.signup',
    userId: 'usr_12345'
  }
});
console.log('Workflow Run ID:', result.runId);
```

#### `workflows.list(params)` & `workflows.activate(id)`
```javascript language-javascript
const workflows = await client.workflows.list({ limit: 10 });
await client.workflows.activate(workflows[0].id);
```

---

### 5. `client.runs`

#### `runs.listByWorkflow(workflowId)` & `runs.cancel(workflowId, runId)`
```javascript language-javascript
const runs = await client.runs.listByWorkflow('wf_998877');
await client.runs.cancel('wf_998877', runs[0].id);
```
