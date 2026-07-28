# Python SDK Reference (`crescendo-sdk-python`)

The official `crescendo` Python SDK provides a native, type-annotated client for interacting with the Crescendo REST API in Python 3.10+ environments.

## Installation

Install the package via pip:

```bash language-bash
pip install crescendo-sdk
```

## Initialization

Import the `Crescendo` class and instantiate the client with your secret API key (starting with `cm_sk_` or `re_`).

```python language-python
from crescendo import Crescendo

client = Crescendo(api_key="cm_sk_live_1234567890abcdef")
```

Alternatively, omit the `api_key` argument to load key values automatically from the `CRESCENDO_API_KEY` environment variable:

```bash language-bash
export CRESCENDO_API_KEY="cm_sk_live_1234567890abcdef"
```

```python language-python
from crescendo import Crescendo

client = Crescendo() # Automatically reads CRESCENDO_API_KEY
```

## Exception & Error Handling

All non-2xx API responses raise a `CrescendoError` exception containing the status code and decoded response body:

```python language-python
from crescendo import Crescendo, CrescendoError

client = Crescendo(api_key="cm_sk_live_12345")

try:
    response = client.emails.send(
        from_address="notifications@verified-domain.com",
        to="user@example.com",
        subject="Welcome to Crescendo",
        html_body="<h1>Hello World</h1>"
    )
    print("Sent email ID:", response.get("id"))
except CrescendoError as e:
    print(f"API Error [{e.status_code}]: {e}")
    print("Raw Error Response:", e.response)
except Exception as e:
    print("Unexpected Client Execution Exception:", e)
```

---

## Service Modules & Methods

### 1. `client.emails`

#### `send()`
Dispatches a single email message.

```python language-python
response = client.emails.send(
    from_address="sales@domain.com",
    to="prospect@client.com",
    subject="Proposal Discussion",
    html_body="<p>Please review our attached proposal.</p>",
    text_body="Please review our attached proposal.",
    email_type="TRANSACTIONAL", # "TRANSACTIONAL" or "MARKETING"
    tags={"campaign": "q3_proposals", "owner": "sarah"}
)
```

#### `send_templated()`
Dispatches an email utilizing a snapshot of a published template ID.

```python language-python
response = client.emails.send_templated(
    from_address="news@domain.com",
    to="subscriber@example.com",
    template_id="3fa85f64-5717-4562-b3fc-2c963f66afa6",
    variables={
        "FIRST_NAME": "Marcus",
        "ACCOUNT_TYPE": "Pro Plan"
    }
)
```

#### `send_batch()`
Sends messages to multiple recipients in a single HTTP request (up to 100 per batch).

```python language-python
response = client.emails.send_batch(
    from_address="alerts@domain.com",
    subject="Security Bulletin Notice",
    html_body="<p>Security update deployed.</p>",
    recipients=[
        "user1@example.com",
        "user2@example.com",
        {"to": "vip@example.com", "custom_tag": "priority"}
    ]
)
```

#### `list()`
Lists historical message logs with status and date filtering options.

```python language-python
logs = client.emails.list(
    status="DELIVERED", # "SENT", "DELIVERED", "FAILED", "BOUNCED"
    limit=50,
    tags={"campaign": "q3_proposals"}
)
```

---

### 2. `client.contacts` (Audiences)

#### `upsert()`
Creates or updates a contact record using their email address.

```python language-python
contact = client.contacts.upsert(
    email="elena@company.com",
    firstName="Elena",
    lastName="Rostova",
    customProperties={
        "signup_date": "2026-07-28",
        "tier": "enterprise"
    }
)
```

#### `set_property()`
Sets a single custom property on a contact, driving automated dynamic audience segmentation rules.

```python language-python
client.contacts.set_property("elena@company.com", "lead_score", 95)
```

---

### 3. `client.templates`

#### `create()`, `publish()`, and `test_send()`
```python language-python
# Create draft template
template = client.templates.create(
    name="Welcome Onboarding Email",
    subject="Welcome {{FIRST_NAME}}",
    html_body="<h1>Hello {{FIRST_NAME}}</h1>",
    variables=[{"name": "FIRST_NAME", "type": "STRING"}]
)

# Publish snapshot
client.templates.publish(template["id"])

# Send test simulation
client.templates.test_send(
    template["id"],
    to_address="qa-team@company.com",
    variables={"FIRST_NAME": "Test User"}
)
```

---

### 4. `client.workflows` & `client.runs`

#### `workflows.trigger()` and `runs.get()`
```python language-python
# Trigger active workflow execution
result = client.workflows.trigger(
    "wf_88776655",
    triggerData={"order_id": "ord_990011"}
)

# Inspect run details
run_info = client.runs.get("wf_88776655", result["runId"])
print("Run Status:", run_info.get("status"))
```
