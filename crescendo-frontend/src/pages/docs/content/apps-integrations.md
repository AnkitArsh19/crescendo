# Apps & Integrations

Crescendo's dynamic integration architecture allows you to connect a vast ecosystem of third-party applications without writing custom integration code. 

## Dynamic App Catalog

Our platform discovers apps, triggers, and actions via a dynamic registry. This means new integrations can be added rapidly by simply defining their configuration schemas (JSON) and mapping the corresponding backend handlers.

### Built-In Integrations

#### 1. Communication & Collaboration
- **Slack:** Send messages, create channels, and listen for mentions.
- **Discord:** Post messages to channels via Webhooks.
- **Gmail & Outlook:** Send transactional or marketing emails, trigger workflows on new incoming emails.

#### 2. Developer & Data Tools
- **GitHub & GitLab:** Trigger workflows on issue creation, PR merges, or push events. Create issues and manage repositories.
- **PostgreSQL:** Execute raw queries or sync data securely.

#### 3. AI & Logic
- **OpenAI & Gemini:** Generate text, analyze payloads, and route logic using intelligent models.
- **Sarvam AI:** Localized, specialized AI actions.

#### 4. Webhooks & HTTP
- **Custom Webhooks:** Expose a unique Crescendo endpoint to ingest data from any system.
- **HTTP Request:** Make arbitrary GET/POST/PUT/DELETE requests to any public API directly from a workflow step.

## Authentication Methods

Integrations require different types of authentication. Crescendo handles these securely:

1. **OAuth 2.0:** For apps like Slack and GitHub. You will be redirected to the provider to authorize Crescendo. We securely store the refresh tokens and automatically rotate short-lived access tokens behind the scenes.
2. **API Keys:** For apps like OpenAI or Custom HTTP endpoints. Keys are encrypted at rest using AES-256 before being stored in our PostgreSQL database.

> [!NOTE]
> Have a custom internal tool? Use the generic **HTTP Request** action to orchestrate workflows against your own private APIs.
