# Python SDK

The `crescendo` package is the hand-written Python client for Crescendo’s public API. It supports Python 3.9 and later.

## Install and configure

```bash
pip install crescendo
export CRESCENDO_API_KEY='re_...'
```

```python
from crescendo import Crescendo

client = Crescendo()  # reads CRESCENDO_API_KEY
```

You may instead pass `api_key=` explicitly from your deployment’s secret configuration. To use a non-production endpoint, set `CRESCENDO_BASE_URL` before importing the package.

## Send an email

```python
from crescendo import Crescendo

client = Crescendo()

result = client.emails.send(
    from_address='updates@your-verified-domain.example',
    to='recipient@example.com',
    subject='Your order is confirmed',
    html_body='<p>Thanks for your order.</p>',
    text_body='Thanks for your order.',
    email_type='TRANSACTIONAL',
    tags={'source': 'checkout'},
)

print(result)
```

## Use a template

```python
template = client.templates.create(
    name='Welcome',
    subject='Welcome, {{FIRST_NAME}}',
    html_body='<h1>Welcome, {{FIRST_NAME}}</h1>',
    variables=[{'name': 'FIRST_NAME', 'type': 'STRING'}],
)

client.templates.publish(template['id'])

client.emails.send_templated(
    from_address='updates@your-verified-domain.example',
    to='recipient@example.com',
    template_id=template['id'],
    variables={'FIRST_NAME': 'Asha'},
)
```

## Modules and errors

The Python client exposes `emails`, `templates`, `contacts`, `suppressions`, `domains`, `metrics`, `webhooks`, `workflows`, `connections`, `apps`, and `runs`. Method names follow Python conventions, such as `send_templated`, `test_send`, `list_by_workflow`, and `get_stats`.

```python
from crescendo import CrescendoError

try:
    client.workflows.trigger('<workflowId>', event='payment.succeeded')
except CrescendoError as error:
    print(error.status_code)
    print(error.response)
```

The client sends the payload you pass to it and observes normal API scopes and rate limits. Check the relevant live API reference for required fields before adding a new integration.
