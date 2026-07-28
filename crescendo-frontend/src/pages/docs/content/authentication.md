# Authentication

The Crescendo Public API enforces strict security standards, demanding authorized, granular API keys for all programmatic interactions and external server requests.

## Generating an API Key

You can provision secure programmatic credentials directly within your workspace under **Settings > Developer API**. When registering a secret key, assign explicit role-based scopes matching your exact software operational requirements.

> [!WARNING]
> Maintain strict confidentiality over your API secret keys. Never embed API tokens inside client-side browser scripts, public repository codebases, or frontend JavaScript frameworks. Always proxy external API requests through authenticated, private server infrastructure.

## Making Authenticated Requests

Provide your generated API Key inside the HTTP `Authorization` request header utilizing the standard `Bearer` authentication schema. Our production API credentials typically initiate with an explicit `re_` prefix string.

```bash language-bash
curl -X GET "https://api.crescendo.run/api/v1/public/workflows" \
  -H "Authorization: Bearer re_1234567890abcdef"
```

```python language-python
import requests

url = "https://api.crescendo.run/api/v1/public/workflows"
headers = {
    "Authorization": "Bearer re_1234567890abcdef"
}

response = requests.get(url, headers=headers)
print(response.json())
```

```javascript language-javascript
fetch('https://api.crescendo.run/api/v1/public/workflows', {
  headers: {
    'Authorization': 'Bearer re_1234567890abcdef'
  }
})
.then(res => res.json())
.then(console.log);
```

## Dashboard Sessions & Internal Cookies

When monitoring web browser HTTP network transmissions, you may observe internal administrative traffic running against endpoints initiated with `/settings/...` or `/api/v1/private/...` utilizing session cookies or short-lived JSON Web Tokens (JWT).

Do not target these browser session endpoints in external custom applications. They are designed strictly to support internal interactive dashboard state mechanics and remain subject to structural updates without general deprecation notification. Always utilize designated `/api/v1/public/...` endpoints for stable, version-controlled developer programming.
