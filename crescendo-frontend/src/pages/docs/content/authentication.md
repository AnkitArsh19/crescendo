# Authentication

The Crescendo Public API uses strictly scoped API Keys for authentication. 

## Generating an API Key

You can generate API Keys directly from the dashboard under **Settings > API Keys**. When creating a key, you must grant it specific scopes.

> [!WARNING]
> Keep your API keys secret. Do not expose them in client-side code, public repositories, or frontend JavaScript frameworks.

## Making Authenticated Requests

Pass your API Key in the `Authorization` header of your HTTP requests. We expect the `Bearer` scheme. Our API keys typically begin with the `re_` prefix.

```bash language-bash
curl -X GET "https://api.crescendo.run/api/v1/workflows" \
  -H "Authorization: Bearer re_1234567890abcdef"
```

```python language-python
import requests

url = "https://api.crescendo.run/api/v1/workflows"
headers = {
    "Authorization": "Bearer re_1234567890abcdef"
}

response = requests.get(url, headers=headers)
print(response.json())
```

```javascript language-javascript
fetch('https://api.crescendo.run/api/v1/workflows', {
  headers: {
    'Authorization': 'Bearer re_1234567890abcdef'
  }
})
.then(res => res.json())
.then(console.log);
```

## Dashboard JWTs

If you are inspecting network traffic in the browser, you may notice routes starting with `/settings/...` that authenticate using a JWT or session cookie. 
**Do not use these endpoints.** They are internal to the Crescendo React Dashboard and subject to unannounced breaking changes. 

Always use `/api/v1/...` for programmatic access.
