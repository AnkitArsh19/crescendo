<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="crescendo-frontend/public/logo-readme-dark.svg">
    <source media="(prefers-color-scheme: light)" srcset="crescendo-frontend/public/logo-readme-light.svg">
    <img alt="Crescendo" src="crescendo-frontend/public/logo-readme-light.svg" width="450">
  </picture>
</p>

# Crescendo

Crescendo is a workflow automation platform built to orchestrate real-world multi-step automations across apps, APIs, and user-defined triggers.

Project status: ongoing. The platform is actively being built, tested, and hardened.

## Why this project was built

Workflow automation is one of the most in-demand product categories right now.

Tools like Zapier, n8n, and Make showed how powerful "if this then that" systems can be, but they also sparked curiosity about what happens behind the scenes: queueing, retries, distributed locks, event pipelines, consistency, security, and scale.

This project was built as a learning and engineering challenge to go beyond a basic CRUD app and design something closer to an L4-level product:

- Not only for resume value
- Not only for personal experimentation
- Built with production-style architecture in mind
- Designed to serve real users and real workflows

The core motivation: learn modern architecture, system design, reliability engineering, and platform-level thinking by building an actual automation system end to end.

## Product vision

Crescendo aims to be a robust automation engine where users can:

- Connect apps and services (OAuth, API keys, webhooks)
- Define workflows with triggers and actions
- Run executions asynchronously and reliably
- Observe workflow/step logs and status transitions
- Scale execution safely with locking, retries, and stream-based processing

The long-term goal is platform thinking, not one-off workflow demos:

- Add integrations quickly without rewriting core orchestration logic
- Keep runtime behavior configuration-driven (not hardcoded)
- Support real-user use cases with reliability and security guarantees

## Dynamic integration architecture

One of the main engineering goals is extensibility.

- Dynamic app catalog: app metadata (apps, triggers, actions, schemas) is loaded from catalog/config data.
- Dynamic frontend rendering: the frontend reads action and configuration schemas to render forms and workflow step configuration UI.
- Dynamic action dispatch: backend action handlers are discovered/registered via mapping annotations and registry patterns, instead of a giant hardcoded switch.
- Contributor-friendly model: developers can add new apps/triggers/actions by extending catalog + handlers with minimal core changes.

This is designed so any developer can add an integration independently and contribute it back.

## High-level architecture

Crescendo is organized as a full-stack monorepo:

- `crescendo-backend`: Spring Boot automation engine and APIs
- `crescendo-frontend`: React + Vite workflow builder and management UI
- `crescendo-aiml`: FastAPI Python service powering the Natural Language Workflow Builder via Groq/LLaMA
- `domain-connect`: Domain Connect JSON templates for automatic DNS configuration
- Root docs and references: architecture notes, production issues, integration guides

Execution flow (simplified):

1. Trigger enters system (polling, event, webhook)
2. Command side persists intent and emits events
3. Outbox/event pipeline publishes to Redis streams
4. Stream consumers process queues with manual ACK for critical paths
5. Workflow engine executes steps and updates run logs
6. Query/read models and caches serve UI and dashboard reads

Engineering focus areas:

- Event-driven asynchronous execution
- Consistency and failure recovery
- Idempotent/defensive processing in queue consumers
- Clear separation of command/write and query/read responsibilities

## Technologies, frameworks, and tools used

### Backend

- Java 25
- Spring Boot 4
- Spring MVC
- Spring Data JPA + JDBC
- Spring Security
- Spring OAuth2 Client
- Spring Validation
- Spring Mail
- Spring Scheduler (`@Scheduled`)
- PostgreSQL
- Redis (streams, cache, lock backing store)
- JWT (`io.jsonwebtoken`)
- ZXing (QR generation)
- Maven Wrapper (`mvnw`)

### Frontend

- React 19
- Vite 7
- React Router
- Zustand (UI state)
- React Query (`@tanstack/react-query`) — server-state caching, optimistic updates
- Axios
- React Hook Form + Zod
- Framer Motion
- React Icons
- XYFlow/React (node/flow style workflow UI)
- ESLint 9

### AI/ML Microservice (`crescendo-aiml`)

- **FastAPI & Uvicorn**: Asynchronous web server and API framework.
- **LangGraph**: Framework for building stateful, multi-agent AI applications.
- **Redis Checkpointer**: Distributed memory management for multi-turn conversational AI context.
- **Groq API**: Lightning-fast LLM inference utilizing `llama-3.1-8b-instant` and `llama-3.3-70b-versatile`.
- **Pydantic**: Strict schema validation to guarantee AI JSON outputs conform to the Java backend contracts.

### Infrastructure and developer tooling

- Docker
- Docker Compose
- Redis container and PostgreSQL container for local/dev
- Git + GitHub repo workflow
- VS Code-based development workflow
- Integration test scripts (Node-based in `integration-tests`)

### Integrations ecosystem (documented/used)

- OAuth apps and APIs like Google, Slack, Discord, Spotify, Microsoft, GitHub, GitLab, LinkedIn, Airtable, Notion, Linear, Strava, Twitter/X
- API-key based integrations like OpenAI, Gemini, Sarvam AI, Toggl, Webhook/HTTP
- No-auth/public integrations like RSS, Weather, NASA APOD, JokeAPI, CatFacts, Giphy, LeetCode, Quotes

### Built-in transactional email platform

Crescendo includes a production-grade transactional email subsystem designed to guarantee deliverability at scale and strictly enforce legal compliance, utilizing a 5-layer architecture akin to enterprise ESPs:

1. **Identity & Usage-Type Binding:** Strict enforcement of SPF, DKIM, and DMARC verification. Root domains can be used (e.g., `company.com`), but they are strictly bound to an `AllowedEmailType` (e.g., `TRANSACTIONAL_ONLY`). The system automatically blocks marketing sends from transactional domains to protect the sender's core reputation.
2. **Multi-Provider BYOK (Bring Your Own Key):** Users can seamlessly connect their own SendGrid (or SES/Postmark) credentials to bypass platform shared IPs and retain their own domain reputation, with automatic fallback to platform sending if the connection fails.
3. **Warming & Rate Governance:** A scheduled daily job that evaluates rolling 48-hour windows of bounces and complaints. New domains start at a strict 50 emails/day cap and exponentially double until maturity, with automatic downgrades if reputation spikes occur.
4. **The Send Decision Gate & Content Heuristics:** A centralized chokepoint (`SendEligibilityService`) that validates domain readiness, daily caps, and usage-type bindings. Additionally, a **draft-time heuristic engine** checks marketing emails for spam triggers (low text-to-image ratio, missing plain text, spam phrases) to protect users before they send.
5. **Provider Abstraction & Idempotency:** An `EmailProvider` interface allows swapping delivery backends. It passes internal idempotency keys to the provider to prevent duplicate sends on network timeouts. It also automatically injects RFC 8058 compliant `List-Unsubscribe` headers and footers to all marketing emails to ensure absolute legal compliance.
6. **Feedback Ingestion & Suppression Portability:** Integrated webhooks capture delivery, bounce, and spam complaint payloads, translating opaque provider errors into plain-language feedback. The platform distinguishes between *hard bounces* and *soft bounces*, and supports multipart CSV and JSON bulk imports so users can migrate suppression lists without friction.
7. **Developer Experience & Tooling:** Crescendo offers zero-dependency native SDKs for Node.js (`@crescendo/email`) and Python (`crescendo`), along with auto-generated SDKs across six languages (Java, PHP, Go, Rust, Ruby, .NET) via an automated OpenAPI CI pipeline. It features a full CLI (`crescendo-cli`), an advanced React Email-powered `TemplateBlockEditor` for creating beautiful emails in the browser, and an MCP (Model Context Protocol) server for native AI-agent integration.
8. **Domain Management & Claiming:** Robust domain control including tracking toggles, custom unsubscribe branding, BIMI record generation, and a secure Domain Claim mechanism to transfer ownership of verified domains between users without complex organizational structures.

This email system is part of the core platform roadmap, not an afterthought.

### 7. Public API Governance & Contract Stability

To provide true Resend/Stripe-level parity, Crescendo exposes its email orchestration (Domains, Audiences, Suppressions) via a public REST API. The design deliberately prioritizes external developer experience and backwards compatibility:

- **Isolated API Surface:** Public endpoints (`/api/v1/*`) are entirely decoupled from internal dashboard routes (`/settings/*`). This ensures internal UI changes never inadvertently break the public contract.
- **Strict Idempotency:** A custom `IdempotencyFilter` caches `POST` responses for 24 hours. Crucially, if a client reuses an `Idempotency-Key` but changes the request payload, the API explicitly returns a `409 Conflict` (like Stripe) rather than silently serving the wrong cached response.
- **Opaque Cursor Pagination:** All list endpoints return a `{ data: [...], has_more: boolean, next_cursor: string }` envelope. While the v1 implementation relies on simple offsets internally, the base64-encoded `next_cursor` hides this from the client. This allows the backend to transparently swap to O(1) keyset pagination as audience sizes scale to millions, with zero API breaking changes.
- **Unified Error Shapes:** A scoped `@RestControllerAdvice` ensures any exception thrown in the `/api/v1/**` namespace is translated into a predictable `{ "type": "...", "message": "...", "status": 4xx }` shape.

### 8. Universal SDK Ecosystem & Multi-Repo Architecture

Great APIs require great client libraries. We provide an ecosystem of 8 officially supported SDKs, which are hosted in a separate dedicated repository: **[Crescendo SDKs (crescendo-sdk)](https://github.com/AnkitArsh19/crescendo-sdk)**.

- **Hand-written DX:** For our most critical ecosystems (Node.js/TypeScript and Python), the SDKs are meticulously hand-crafted to provide a zero-dependency, highly idiomatic developer experience.
- **Automated Generation at Scale:** For Java, Go, Rust, PHP, Ruby, and .NET, we utilize a fully automated `openapi-generator-cli` pipeline.
- **Cross-Repo CI/CD:** To prevent the massive volume of auto-generated code (240,000+ lines) from bloating the backend git history, the architecture is strictly decoupled. When the Spring Boot backend CI detects an API surface change, it spins up an ephemeral, isolated backend instance (with its own PostgreSQL and Redis service containers), extracts the generated `openapi.json` spec, and securely pushes it directly into the `crescendo-sdk` repository. This triggers the SDK generation pipeline downstream, completely isolating the generated noise from the core engine and avoiding any reliance on live, deployed environments.

### 9. AI/ML Integration: Design-Time and Run-Time Agents

Crescendo features a robust AI/ML microservice (`crescendo-aiml`) that powers two distinct agentic paradigms:

**1. The Natural Language Workflow Builder (Design-Time)**
Translates conversational user intents into fully executable workflows via a multi-stage LangGraph pipeline:
- **Stateful Conversational Memory:** Utilizes a Redis checkpointer to remember multi-turn conversational history when clarifying ambiguous prompts, eliminating the need to reset context.
- **Intent Classification & Strict Clarification:** Rapid processing (Llama-3.1-8b) to enforce clarity. If a user provides an ambiguous prompt lacking specific application names, the model strictly halts and asks clarifying questions instead of hallucinating configurations.
- **True DAG Conditional Branching:** The LLM goes beyond linear steps and correctly generates multi-branch Directed Acyclic Graphs with logical conditions natively mapped to the execution engine.
- **Deterministic Catalog Validation:** A pure-Python validation layer ensures the generated workflow strictly conforms to the backend's known catalog schemas and dynamic user resources.

**2. The Agentic AI Node (Run-Time)**
A first-class workflow node that evaluates incoming payloads against a system prompt and dynamically selects tools to call using a ReAct loop:
- **Stateless Python Reasoning:** The Java engine maintains loop state, security, and idempotency, calling a stateless Python endpoint (`/v1/agent/next-step`) for reasoning decisions on each turn.
- **Pre-Execution Budgeting & Context Windows:** Token budgets are enforced *before* any LLM calls, and a sliding context window ensures long-running agent loops never exceed Groq API token limits.
- **Strict Schema Enforcement:** Dynamically filters tool access so the agent only sees the tools explicitly connected by the user on the canvas, while rigorously validating required arguments.


## Design patterns and architectural patterns implemented

Crescendo intentionally uses production-style patterns instead of simple request-response CRUD only.

### 1. CQRS-style separation

- Clear `command` and `query` model separation across modules
- Different write/read responsibilities to support scaling and cleaner boundaries

### 2. Event-driven architecture

- Domain events and stream-based consumers
- Asynchronous processing pipelines for workflow execution and side effects

### 3. Transactional Outbox pattern

- Outbox table + scheduled publisher
- Reliable publish-after-commit behavior for stream/event delivery

### 4. Consumer Group + Manual ACK strategy

- Critical execution queue uses manual ACK to avoid message loss
- Less critical streams can use auto-ack where acceptable

### 5. Pending-entry recovery (PEL reclaim)

- Reaper process to claim stalled pending messages and reprocess them
- Protects against consumer crashes leaving zombie messages

### 6. Dead Letter Queue (DLQ) + retries

- Failed stream messages moved to DLQ
- Retry counters and backoff behavior for resilience

### 7. Distributed lock pattern with token ownership

- Redis lock with unique token per acquisition
- Atomic Lua-script-based unlock/extend for correctness
- Lock heartbeat extension for long-running executions

### 8. Scheduler-driven reliability jobs

- Outbox publisher loops
- Stream container health monitoring and restart checks
- Orphan/stuck run recovery jobs

### 9. Repository-Service-Controller layering

- Separation of persistence, business logic, and HTTP interface concerns

### 10. Defensive webhook ingestion

- Signature verification for webhook authenticity
- Safe payload parsing and guarded trigger execution

### 11. Configuration-driven workflow steps

- Action input schemas and templates are designed so behavior is configured per step
- Reduces hardcoded action wiring in business flows
- Makes new integration onboarding faster for contributors

### 12. Directed Acyclic Graph (DAG) Execution & Edge-State Routing

- **Edge-State Routing Model**: Execution state belongs to individual graph edges (`sourceId:targetId:handle`), not just target nodes. Edges transition between `ST_PENDING`, `ST_COMPLETED`, and `ST_SKIPPED`. If/Else (`logic:if`) and Switch (`logic:switch`) branch steps mark the selected output handle as `ST_COMPLETED` and untaken handles as `ST_SKIPPED`.
- **Natural Skip Cascading**: Eliminates fragile recursive DFS walks. In topological order (Kahn's algorithm), a node only executes when non-skipped incoming edges complete; if all incoming edges are `ST_SKIPPED`, the node marks itself `ST_SKIPPED` and cascades `ST_SKIPPED` forward to its outgoing edges.
- **Deterministic Merge Join (`logic:merge`)**: Handles reconverging parallel branches by collecting and flattening all completed parent outputs using deterministic step UUID ordering. Emits audit warning logs on key collisions and preserves unmerged per-parent data under `_bySource`.
- **Recursive Expression Resolver**: `WorkflowExpressionResolver` walks deeply into nested maps, lists, `$ref` objects, and template strings, preserving native JSON types (`number`, `boolean`, `object`, `array`, `null`) before logic evaluation.
- **Persisted Routing State**: Edge decisions (`_edgeState`) are saved in `executionState` on run suspension and restored seamlessly when a suspended workflow run resumes.
- **Visual Rule Builder UI**: Frontend dynamic node type resolution (`resolveNodeType`) ensures `BranchNode` renders with named output handles (`true`/`false`, `output_N`) immediately upon creation. Config panel features `ConditionRuleBuilder` for visual group editing (AND/OR) and switch routing rules with a raw JSON editor fallback.

### 13. Native Postgres Search & Async Batched Rollups

- Eliminated the need for Elasticsearch or Datadog by implementing high-performance search and metrics natively.
- **Async Rollups**: Solved heavy write-throughput and hot-row contention for time-series data using a Redis Stream consumer that flushes batched metrics every 5 seconds.
- **Postgres Search**: Utilized `tsvector` and `pg_trgm` extensions to achieve highly efficient, relevance-ranked full-text search across millions of logs.
- Why Postgres serves the purpose: No data synchronization delays, no split-brain schema issues, and significant operational simplicity compared to managing a separate ELK stack.

### 14. Passwordless WebAuthn & Passkeys

- **FIDO2 Cryptography**: Full support for hardware keys and biometric passkeys (FaceID/TouchID) using public-key cryptography via `webauthn4j`.
- **Verified-Email-First Signup**: Passkey-only registration uses a strict OTP-first verification flow. This eliminates account-takeover vectors by ensuring the user proves ownership of the email *before* the server registers the public key or activates the account.
- **Stateful Security Matrix**: The platform gracefully handles complex credential matrix scenarios (e.g., preventing users from deleting their final passkey if they have no password fallback, bypassing TOTP prompts when a passkey satisfies MFA inherently).
- **Device & Identity Limiting**: Public passkey and recovery endpoints are protected by a two-layer rate limiter (IP-based volumetric limits + identity-keyed credential stuffing limits) that avoids consuming the request body during denial.

### 15. Intelligent Sign-in Detection & Session Revocation

- **Smart Login Alerts**: Moving beyond basic "new login" alerts which cause alert fatigue, the platform detects anomalous logins by analyzing the combination of device fingerprinting and GeoIP location data.
- **Symmetric Anomaly Detection**: Triggering alerts on a strict OR-gate (New Device OR New Location) prevents loopholes where an attacker on a new device but same VPN location goes undetected.
- **Cross-Stack Device Fingerprinting**: Device UUIDs are generated client-side and persisted in `localStorage`. These are threaded through all login paths including passwords, passkeys (`X-Device-Id` headers), and OAuth flows (`SameSite=Lax` transfer cookies) to ensure comprehensive attribution.
- **Stateless Revocation & Bounded Exposure**: Instead of a complex Redis blocklist for revoked JWTs, the system employs short-lived Access Tokens (15m) and long-lived Refresh Tokens. Revocation instantly kills the refresh token in the database, relying on the short TTL to bound the exposure window gracefully.

### 16. Layered Client-Side Caching with SSE-driven Invalidation

- **Two-Layer Cache Architecture**: The backend serves Redis-cached responses (per-user TTLs, event-driven eviction via `@TransactionalEventListener`). The frontend adds React Query as a second, independent cache layer — the two solve different costs: backend reduces DB load, frontend eliminates redundant network round-trips on navigation.
- **Correct staleTime semantics**: The workflow list uses a short `staleTime` (30s) for freshness on re-visits. The open canvas uses `staleTime: Infinity` — no background refetch mid-edit — with `refetchOnWindowFocus: 'always'` as the intentional safety net.
- **Optimistic Mutations with Rollback**: Activate/deactivate and workflow renames update the React Query cache immediately and roll back on failure via `onMutate`/`onError`/`onSettled` lifecycle, matching the UX standard set by tools like Notion and Linear.
- **SSE Push Channel + Redis Pub/Sub Fan-out**: `WorkflowSseService` holds per-instance SSE emitters. Mutations publish to a Redis Pub/Sub channel (`workflow-events:{userId}`). Every backend instance subscribes and fans notifications to its own locally registered emitters. This ensures cross-tab and multi-instance invalidation without the per-request blocklist overhead.
- **Layered Redundancy for Disconnects**: `EventSource` reconnects automatically after network drops. Events missed during a disconnect are caught by `refetchOnWindowFocus`, which fires on laptop wake. The two mechanisms are complementary, not redundant.

### 17. OAuth 2.0 Authorization Server & Personal Custom OAuth Apps (BYOA)

- **Crescendo as OAuth 2.0 Provider**: Implements a full RFC 6749 / RFC 7636 Authorization Server using Spring Authorization Server. External developer applications can authenticate via PKCE authorization code flows to securely access `/api/v1/*` public APIs with fine-grained granular scopes (`workflow:read`, `email:send`, `domains:write`, `connections:read`, etc.).
- **Bring-Your-Own-App (BYOA) Integration**: Users can register their own custom OAuth App credentials (Client ID & Client Secret) for third-party SaaS providers (Google, Slack, GitHub, Spotify, etc.) directly in Settings. The token exchange and automated background token refresh pipelines (`OAuthTokenRefreshService`) dynamically resolve and inject the user's encrypted custom credentials instead of shared platform defaults, giving developers full ownership over quotas and consent screens.

### 18. Universal Dynamic Resource Resolution & Cascading Dropdowns (`ResourceProvider`)

- **Zero Manual ID Entry**: Eliminates fragile copy-pasting of opaque entity IDs (`spreadsheetId`, `sheetId`, `databaseId`, `pageId`, `bucket`, `objectKey`, `contactId`, `ticketId`, `channelId`, `jobPath`) across all 114 application integrations.
- **Provider SPI & Cache Architecture**: Integrations implement a decoupled `ResourceProvider` SPI defining `supportedResourceTypes()` and `contextResourceDescriptors()`. Results are fetched via authorized API requests and cached with granular TTLs in Redis to prevent API rate limits.
- **Cascading Dependencies (`dependsOn`)**: Form fields declare dependent relationships (e.g. S3 Bucket $\rightarrow$ S3 Objects, Notion Database $\rightarrow$ Pages, Mattermost Team $\rightarrow$ Channels, Jenkins Folder $\rightarrow$ Jobs). When a parent resource changes, the frontend automatically invalidates and cascades queries to fetch child resources.

### 19. Safety-First Step Testing & Real-Time Expression Resolution Engine

- **Elimination of Blind Mutations**: Solved the classic integration platform flaw where "testing a step" inadvertently creates dummy records, charges credit cards, or sends premature customer messages.
- **3-Tier Testing Architecture**:
  1. *Check Setup (Non-mutating Pre-Flight)*: Validates connection health, required scopes, mandatory fields, and proves target resource existence using `ResourceProvider` probes without executing mutating HTTP verbs or action handlers.
  2. *Trigger & Read-Only Sample Fetching*: Retrieves safe sample records (`/trigger-sample` and `/read-sample`) for downstream variable mapping.
  3. *Gated Live Execution*: Real handler execution (`/live-run`) is strictly isolated behind an explicit confirmation modal with mandatory side-effect acknowledgment.
- **Runtime Expression Evaluation Preview**: `WorkflowExpressionResolver.resolveForTest()` evaluates complex variable mappings (`{{steps.1.customerEmail}}`, `{{now}}`, `{{today}}`, `{{timestamp}}`) against sample input data in real-time, displaying the exact resolved "Data In" payload with native type preservation.
- **Upstream Test Data Chaining**: Step test outputs are saved to canvas nodes, allowing downstream steps to immediately select upstream records (`[Use Step 1 Output]`) without manual JSON copy-pasting.

### 20. Real-Time Notification Inbox, SSE Streaming & Alert Noise Governance

- **Persistent In-App Inbox**: Indexed PostgreSQL storage with composite index `(userId, isRead, createdAt)` providing performant paginated inbox queries, real-time unread badge counts, and optimistic bulk/single mark-as-read.
- **Cross-Instance SSE via Redis Pub/Sub**: System events (workflow execution outcomes, AI draft completions, suspicious logins, MFA toggles, OAuth token expirations) persist a notification record and publish it to Redis Pub/Sub (`user-notifications:{userId}`). Clustered backend nodes fan out events directly to active client `EventSource` connections (`/notifications/events`) without poll-based database load.
- **HTML5 Desktop Notifications**: Native browser `Notification` API delivery alerts users when backgrounded tabs are running, without requiring external push gateways or heavyweight client daemons.
- **Per-Workflow Alert Granularity & Noise Control**: Prevents notification fatigue for high-frequency or batch workflows with per-workflow alert rules (`ALWAYS`, `FAILURE_ONLY`, `NEVER`) alongside per-category event opt-ins.
- **Automated Retention Management**: A nightly `@Scheduled` background worker purges notification records older than the configured retention horizon (default 90 days), maintaining bounded storage growth.

## Reliability and production-style concerns addressed

- Duplicate publish/race prevention with pessimistic locking on outbox reads
- Cache eviction strategy improved from blanket eviction to targeted invalidation
- Reduced sensitive token leakage in non-local contexts
- Stream listener container health checks and restart logic
- Recovery for stuck pending execution messages
- Heartbeat extension for lock TTL during long workflows

## Local development

### Quick Start Script (Windows)

Launch all microservices concurrently in organized Windows Terminal tabs with a single command:

```powershell
.\dev.ps1
```

This automatically orchestrates:
- **Backend (Spring Boot)** -> `http://localhost:8080`
- **AI/ML Engine (FastAPI)** -> `http://localhost:8000`
- **Frontend (Vite / React)** -> `http://localhost:5173`

### Manual Setup

1. Start dependencies (PostgreSQL, Redis) via Docker Compose
2. Run backend via `./mvnw spring-boot:run` from `crescendo-backend`
3. Run AI/ML service via `uvicorn app.main:app --port 8000` from `crescendo-aiml`
4. Run frontend via `npm install && npm run dev` from `crescendo-frontend`

### Production Deployment (Docker Compose + Cloudflare Tunnel)

Crescendo is containerized for production on an AWS EC2 instance (`c7i-flex.large`: 2 vCPU, 4 GiB RAM) behind Cloudflare Tunnel with **zero public open host ports**:

1. **Configure Environment Secrets:**
   ```bash
   cp .env.example .env
   # Populate .env with production database credentials, JWT secret, and OAuth keys
   ```

2. **Launch Production Stack:**
   ```bash
   docker compose up -d --build
   ```
   This orchestrates:
   - **PostgreSQL 16** (Internal network only, dual CQRS databases: `crescendo_command` and `crescendo_query`)
   - **Redis 8** (Internal network only, AOF persistence + `noeviction` stream protection)
   - **Backend Engine (Java 25 Spring Boot)** (`-Xms256m -Xmx768m`, HikariCP 8+8 connections)
   - **AI/ML Service (Python FastAPI + LangGraph)** (Direct Docker network routing)
   - **Frontend (React SPA + Nginx)** (Internal network only, port 80)
   - **Prometheus** (Localhost scrape on `127.0.0.1:9090`)
   - **Cloudflare Tunnel (`cloudflared`)** (Outbound connector routing `app.crescendo.run`, `api.crescendo.run`, `ai.crescendo.run`)

3. **Local Diagnostics & Monitoring (Optional):**
   To launch the developer diagnostic UI (Grafana dashboards, RedisInsight, and host-mapped database ports):
   ```bash
   docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d
   ```
   - **Grafana**: `http://localhost:3001` (HikariCP pool graphs, JVM memory, Virtual Threads)
   - **RedisInsight**: `http://localhost:5540`
   - **Frontend**: `http://localhost:80`
   - **Backend**: `http://localhost:8080`

4. **Automated Database Backups:**
   ```bash
   # Scheduled cron job dumps both databases and syncs to AWS S3 / Cloudflare R2:
   ./scripts/backup-db.sh
   
   # Safe disaster recovery:
   ./scripts/restore-db.sh ./backups/<TIMESTAMP>
   ```

OAuth callback testing and external webhook testing:

- In production, Cloudflare Tunnel securely connects `app.crescendo.run`, `api.crescendo.run`, and `ai.crescendo.run` without requiring any open inbound ports on EC2 security groups.
- This enables realistic OAuth redirect URI testing and end-to-end webhook/provider callback flows.

## Testing strategy & zero-credential verification

A common challenge in integration platforms is testing hundreds of third-party actions without requiring developers to manage dozens of external developer accounts, live API keys, or flaky OAuth refresh tokens in CI.

Crescendo utilizes a **4-Layer Zero-Credential Verification Strategy**:

1. **Universal Catalog Contracts (`CatalogContractTest`, `OperationTestContractFactoryTest`)**: All 114 application integrations and 868 action mappings are loaded into memory and audited automatically in ~100ms. Asserts unique app/action keys, parameter schema types, test policy assignments (`READ_TARGET`, `READ_SAMPLE`, `LOCAL_SIMULATION`), handler registration parity, and valid JSON payload formatting without network connectivity or API tokens.
2. **Step Setup & Non-Mutating Validation Suite (`StepSetupValidationServiceTest`, `StepTestControllerTest`)**: Verifies that step testing routes never fall back to admin credentials, execute local logic simulations safely, and resolve expression templates against sample input data.
3. **Local HTTP Mock Seams (`ResourceProviderHttpContractTest`, `TriggerSampleServiceTest`)**: Verifies dynamic resource fetching (Gmail inboxes, Spotify playlists, Slack channels) and trigger sample generation by spinning up lightweight embedded local web servers. By feeding dummy tokens (`"test-token-123"`) to the Java providers, tests assert precise OAuth Bearer token headers and UI dropdown serialization without calling external servers.
4. **Native Email Pipeline Testing**: Transactional HTML template rendering (`EmailTemplateRendererTest`) and cryptographic DNS authentication strings for SPF, DKIM, and DMARC (`DnsVerificationServiceTest`) run completely in-memory in CI, while physical inbox placement and bounce webhooks are validated against a dedicated test subdomain.

Running tests locally requires **no user accounts, no active OAuth configurations, and zero secret keys in `application.properties`**:

```bash
# Run backend contract & execution suites offline
cd crescendo-backend && ./mvnw test

# Run frontend Vitest unit & component suites offline
cd crescendo-frontend && npm test -- --run
```

## Performance Hardening, Concurrency & Telemetry Suite

Crescendo is architected for enterprise-scale concurrent traffic and resilient load testing without cloud vendor dependencies:

- **Java 21+ Virtual Threads & Custom Schedulers:** Core networking and async thread pools operate on Virtual Threads (`spring.threads.virtual.enabled=true`). Because Spring Boot does not automatically convert manually instantiated executors, background tasks like workflow distributed lock extension heartbeats (`ExecutionQueueConsumer`) and interval schedules (`SchedulerConfig`) explicitly employ `Thread.ofVirtual().factory()`. This ensures thousands of background timers consume virtually zero OS platform thread memory.
- **HikariCP Sizing & OSIV Boundary:** By explicitly disabling Open-in-View (`spring.jpa.open-in-view=false`), database connections are borrowed strictly during `@Transactional` queries and released immediately before network proxying or JSON rendering. Coupled with conservative local pool formulas (`Pool Size = ((Cores * 2) + Spindles)`), Crescendo handles high concurrency without causing Docker RAM starvation or OS memory swapping.
- **Live Telemetry & Observability:** Integrated Spring Boot Actuator and Micrometer metrics stream real-time operational state to Prometheus and Grafana. Running `docker-compose up -d prometheus grafana` launches a pre-configured dashboard at `http://localhost:3001` showing live HikariCP pool usage, active HTTP sockets, requests per second (RPS), and JVM garbage collection intervals.
- **Race-Condition & Double-Execution Defense:** To guarantee protection against silent double-execution defects—where two simultaneous workers both assume lock acquisition and silently return HTTP 200 without raising server exceptions—Crescendo implements a dual-layer verification strategy:
  - **In-Memory JVM Concurrency Suite:** Automated integration tests (`DistributedLockServiceIntegrationTest`) fire 100 synchronized Virtual Threads directly in RAM across a `CountDownLatch` starting barrier, asserting exactly 1 worker acquires the lock and 99 fail cleanly.
  - **3-Tier Container Benchmark Suite (`performance-tests/`):** Standalone k6 configurations test peak Read queries, transactional Write bursts, and simultaneous race conditions via simple Docker execution commands without local software installation.

## What makes this project different from a basic CRUD app

- Asynchronous queue-driven execution engine
- Reliability primitives (outbox, lock ownership, manual ACK, DLQ, PEL reclaim)
- Event and workflow orchestration mindset
- Integration-heavy platform surface
- Product-oriented architecture choices for real-user scenarios

## Open source and contributions

This project is being shaped to be developer-extensible and open-source friendly.

- New app integrations should be addable without rewriting the engine
- Configuration/schema-driven UI and handler mapping reduce contributor friction
- Architecture and docs are written to help contributors understand where to extend safely

Contribution direction:

- Add new app definitions (triggers/actions/config schemas)
- Add action/trigger handlers
- Improve reliability jobs and observability
- Expand template library and production hardening

## Recruiter and engineering highlights

This project demonstrates:

- Building distributed, async systems beyond CRUD
- Applying system design patterns (CQRS, outbox, stream consumers, distributed locks)
- Handling real failure scenarios (retries, DLQ, pending reclaim, health checks)
- Designing extensible platform architecture for third-party integrations
- Balancing product velocity with reliability and security foundations
- Making deliberate, reasoned tradeoffs (e.g. two-layer cache over a single approach, SSE + Redis Pub/Sub over per-request blocklists)

## Current direction

Crescendo is being shaped as a practical automation and transactional communication platform with strong engineering foundations, where learning and real-world product quality are both first-class goals.
