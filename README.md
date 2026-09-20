<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="crescendo-frontend/public/logo-readme-dark.svg">
    <source media="(prefers-color-scheme: light)" srcset="crescendo-frontend/public/logo-readme-light.svg">
    <img alt="Crescendo" src="crescendo-frontend/public/logo-readme-light.svg" width="450">
  </picture>
</p>

# Crescendo

<p align="center">
  <a href="https://app.crescendo.run"><strong>Live Web Platform</strong></a> &nbsp;•&nbsp;
  <a href="https://app.crescendo.run/docs"><strong>Documentation Portal</strong></a> &nbsp;•&nbsp;
  <a href="https://app.crescendo.run/docs/public-api"><strong>API Reference</strong></a> &nbsp;•&nbsp;
  <a href="https://github.com/AnkitArsh19/crescendo-sdk"><strong>SDK Ecosystem</strong></a> &nbsp;•&nbsp;
  <a href="https://github.com/AnkitArsh19/crescendo/releases/tag/v1.0.3"><strong>Desktop App (v1.0.3)</strong></a>
</p>

Crescendo is a workflow automation platform built to orchestrate real-world multi-step automations across apps, APIs, and user-defined triggers.

**Live Web Application:** [https://app.crescendo.run](https://app.crescendo.run)  
**Project status: Version 1.0 (v1.0.3) is released and production-ready.** The initial release milestone is complete, with core workflow orchestration, the AI agent runtime, transactional email infrastructure, and native desktop clients fully built and operational. Future development follows a continuous improvement model for enhancements, optimizations, and new catalog integrations.

## Why this project was built

Workflow automation is one of the most in-demand product categories right now.

Tools like Zapier, n8n, and Make showed how powerful "if this then that" systems can be, but they also sparked curiosity about what happens behind the scenes: queueing, retries, distributed locks, event pipelines, consistency, security, and scale.

This project was built as a learning and engineering challenge to go beyond a basic CRUD app and design something closer to an L4-level product:

- Not only for resume value
- Not only for personal experimentation
- Built with production-style architecture in mind
- Designed to serve real users and real workflows

The core motivation: learn modern architecture, system design, reliability engineering, and platform-level thinking by building an actual automation system end to end.

## Product capabilities

Crescendo provides a robust automation engine that allows users to:

- Connect apps and services (OAuth, API keys, webhooks)
- Define workflows with triggers and actions
- Run executions asynchronously and reliably
- Observe workflow/step logs and status transitions
- Scale execution safely with locking, retries, and stream-based processing

The platform architecture is built around platform thinking, not one-off demos:

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
- `crescendo-frontend`: React + Vite workflow builder, management UI ([app.crescendo.run](https://app.crescendo.run)), and Tauri native desktop client
- `crescendo-aiml`: FastAPI Python service powering the Natural Language Workflow Builder and Agentic AI ReAct Runtime (Google Gemini, Groq, OpenAI)
- `domain-connect`: Domain Connect JSON templates for automatic DNS configuration
- Root docs and references: interactive documentation portal at [app.crescendo.run/docs](https://app.crescendo.run/docs), architecture notes, and integration guides

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
- React Query (`@tanstack/react-query`): server-state caching, optimistic updates
- Axios
- React Hook Form + Zod
- Framer Motion
- React Icons
- XYFlow/React (node/flow style workflow UI)
- ESLint 9

### Native Desktop Application (`crescendo-desktop`)

- **Tauri v2**: Next-generation lightweight native application runtime powered by Rust.
- **Rust (2021 edition)**: High-performance, memory-safe OS process management and protocol routing.
- **`tauri-plugin-single-instance`**: Enforces a single application instance, intercepts secondary launches, and routes deep-link CLI parameters to the primary window.
- **`tauri-plugin-deep-link`**: System-level custom URL protocol registration (`crescendo://`).
- **`tauri-plugin-opener`**: Native OS browser and external file invocation.
- **Frameless Glass Titlebar**: Custom window frame with minimize, maximize, restore, close, and drag controls matching the Crescendo design system.

### AI/ML Microservice (`crescendo-aiml`)
 
- **FastAPI & Uvicorn**: Asynchronous web server and high-throughput API framework.
- **Autonomous Agentic AI Node**: Multi-turn ReAct (Reason + Act) loop endpoint (`/v1/agent/next-step`) that dynamically selects, invokes, and observes tool executions from workflow inputs.
- **Multi-Provider Tool-Calling Engine**:
  - **Google Gemini**: Native API function-calling (`gemini-3.5-flash-lite` default with 500 RPD quota; `gemini-3.8-flash` and `gemini-3.6-flash` for admin users).
  - **Groq**: Ultra-low-latency function calling via `llama-3.3-70b-versatile` and `llama-3.1-8b-instant`.
  - **OpenAI**: Flagship agentic models (`gpt-4o`, `gpt-4o-mini`).
- **LangGraph**: Framework for building stateful, multi-agent conversational DAG generators.
- **Redis Checkpointer**: Distributed memory management for multi-turn conversational AI context.
- **Pydantic**: Strict schema validation guaranteeing AI JSON outputs and tool definitions conform to the Java backend contracts.

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
7. **Developer Experience & Tooling:** Crescendo offers zero-dependency native SDKs for Node.js ([`@crescendo/email`](https://app.crescendo.run/docs/sdk-node)) and Python ([`crescendo`](https://app.crescendo.run/docs/sdk-python)), along with auto-generated SDKs across six languages ([Java, PHP, Go, Rust, Ruby, .NET](https://app.crescendo.run/docs/sdk-multi-language)) via an automated OpenAPI CI pipeline. It features a full CLI (`crescendo-cli`), an advanced React Email-powered `TemplateBlockEditor` for creating beautiful emails in the browser, and an MCP (Model Context Protocol) server for native AI-agent integration.
8. **Domain Management & Claiming:** Robust domain control including tracking toggles, custom unsubscribe branding, BIMI record generation, and a secure Domain Claim mechanism to transfer ownership of verified domains between users without complex organizational structures.

This email system is part of the core platform roadmap, not an afterthought.

### 7. Public API Governance & Contract Stability

To provide true Resend/Stripe-level parity, Crescendo exposes its email orchestration (Domains, Audiences, Suppressions) via a public REST API ([API Reference](https://app.crescendo.run/docs/public-api) & [Governance Guide](https://app.crescendo.run/docs/api-governance)). The design deliberately prioritizes external developer experience and backwards compatibility:

- **Isolated API Surface:** Public endpoints (`/api/v1/*`) are entirely decoupled from internal dashboard routes (`/settings/*`). This ensures internal UI changes never inadvertently break the public contract.
- **Strict Idempotency:** A custom `IdempotencyFilter` caches `POST` responses for 24 hours. Crucially, if a client reuses an `Idempotency-Key` but changes the request payload, the API explicitly returns a `409 Conflict` (like Stripe) rather than silently serving the wrong cached response.
- **Opaque Cursor Pagination:** All list endpoints return a `{ data: [...], has_more: boolean, next_cursor: string }` envelope. While the v1 implementation relies on simple offsets internally, the base64-encoded `next_cursor` hides this from the client. This allows the backend to transparently swap to O(1) keyset pagination as audience sizes scale to millions, with zero API breaking changes.
- **Unified Error Shapes:** A scoped `@RestControllerAdvice` ensures any exception thrown in the `/api/v1/**` namespace is translated into a predictable `{ "type": "...", "message": "...", "status": 4xx }` shape.

### 8. Universal SDK Ecosystem & Multi-Repo Architecture

Great APIs require great client libraries. We provide an ecosystem of 8 officially supported SDKs, which are hosted in a separate dedicated repository: **[Crescendo SDKs (crescendo-sdk)](https://github.com/AnkitArsh19/crescendo-sdk)** with step-by-step documentation for [Node.js / TypeScript](https://app.crescendo.run/docs/sdk-node), [Python](https://app.crescendo.run/docs/sdk-python), and [Multi-Language Runtimes](https://app.crescendo.run/docs/sdk-multi-language).

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
A first-class autonomous workflow node (`agent:ai_agent`) that evaluates incoming step payloads against a system prompt, dynamically reasons through a ReAct (Reason → Act → Observe) loop, and calls catalog app tools or sub-workflows:
- **Autonomous Catalog Tool Calling (111+ Apps):** Dynamically synthesizes standard OpenAPI / JSON Schema function parameter objects from Crescendo's catalog `configSchema`s. Sanitizes tool identifiers (`appKey__actionKey`) adhering strictly to LLM provider requirements (`^[a-zA-Z0-9_]{1,64}$`), with bidirectional resolution and snake/camelCase tolerance.
- **3-Tier Credential Resolution:** Seamlessly resolves credentials through `WorkflowExecutionEngine`, properly decrypting the workflow owner's personal OAuth tokens or API keys with graceful platform-key fallback.
- **Sub-Workflow Tool Orchestration:** Allows exposing any workspace workflow as a callable tool, executing child workflows synchronously under distributed Redis locks (`workflow-execution:{subWorkflowId}`) and feeding child outputs directly back into the agent's reasoning context.
- **Dual Execution Engine & Direct REST Fallback:** Connects to the Python AI microservice (`crescendo-aiml`) via `/v1/agent/next-step`, with an automatic, direct native Java REST fallback (Google Gemini `generateContent` with `function_declarations` and OpenAI/Groq `/chat/completions` with `tools`) ensuring high availability even when the Python microservice is unreachable.
- **Defense in Depth & Prompt Injection Defense:** Wraps tool observations in XML boundary delimiters (`<tool_output_content>`), strips prompt injection patterns, and enforces pre-execution token budget caps and iteration limits before any external side effects occur.
- **Visual ReAct Timeline Inspector:** Tracks every turn in a structured execution timeline (`input` → `thought` → `tool_call` → `observation` → `final_answer`), rendered natively in the frontend canvas drawer (`TestResultPanel`) and run history (`RunDetail`) via `AgentTimelineView`.


## Crescendo Desktop (Native Application)

Crescendo Desktop provides a focused, high-performance native workflow automation and orchestration experience across Windows, macOS, and Linux without the memory bloat of Electron.

### Official Download Releases (v1.0.3)

| Operating System | Package Format | Architecture | Size | Direct Download |
|---|---|---|---|---|
| **Windows** | `.exe` (NSIS Setup) | x64 | 4.13 MB | [Download `.exe`](https://github.com/AnkitArsh19/crescendo/releases/download/v1.0.3/Crescendo_1.0.3_x64-setup.exe) |
| **Windows** | `.msi` (Windows Installer) | x64 | 5.09 MB | [Download `.msi`](https://github.com/AnkitArsh19/crescendo/releases/download/v1.0.3/Crescendo_1.0.3_x64_en-US.msi) |
| **macOS** | `.dmg` (Universal Disk Image) | Apple Silicon & Intel | 10.1 MB | [Download `.dmg`](https://github.com/AnkitArsh19/crescendo/releases/download/v1.0.3/Crescendo_1.0.3_universal.dmg) |
| **macOS** | `.app.tar.gz` (App Archive) | Apple Silicon & Intel | 10.1 MB | [Download `.tar.gz`](https://github.com/AnkitArsh19/crescendo/releases/download/v1.0.3/Crescendo_universal.app.tar.gz) |
| **Linux** | `.AppImage` (Standalone) | x86_64 / amd64 | 80.4 MB | [Download `.AppImage`](https://github.com/AnkitArsh19/crescendo/releases/download/v1.0.3/Crescendo_1.0.3_amd64.AppImage) |
| **Linux** | `.deb` (Debian / Ubuntu) | amd64 | 6.42 MB | [Download `.deb`](https://github.com/AnkitArsh19/crescendo/releases/download/v1.0.3/Crescendo_1.0.3_amd64.deb) |
| **Linux** | `.rpm` (Fedora / RHEL) | x86_64 | 6.42 MB | [Download `.rpm`](https://github.com/AnkitArsh19/crescendo/releases/download/v1.0.3/Crescendo-1.0.3-1.x86_64.rpm) |

All official release artifacts are cryptographically signed and available on the **[GitHub Releases](https://github.com/AnkitArsh19/crescendo/releases/tag/v1.0.3)** page.

### Why Tauri v2 over Electron?
- **Ultra-Lean Binary Footprint:** <15 MB total installer size vs. 150+ MB for minimal Electron runtimes.
- **Native OS WebViews:** Utilizes Microsoft WebView2 on Windows, WebKit on macOS, and WebKitGTK on Linux. Consumes only ~35-45 MB RAM idle compared to 350-500 MB in Electron.
- **Rust Process Security:** Strict compile-time memory safety, fine-grained capability boundaries (`capabilities/default.json`), and zero exposed Node.js runtime attack surface in production bundles.

### Enterprise Native Security & Authentication (RFC 8252)
Desktop authentication implements the OAuth 2.0 Best Current Practice for Native Apps ([RFC 8252](https://datatracker.ietf.org/doc/html/rfc8252)):
1. **System Browser Login (No Embedded Webviews):** The desktop app delegates authentication to the user's default browser ([`app.crescendo.run`](https://app.crescendo.run)). This preserves biometric hardware keys (WebAuthn / FIDO2 Passkeys), enables single-click login for existing browser sessions, and guarantees user credentials are never handled directly by native application code.
2. **Ephemeral 60s Handoff Code:** Upon web authentication, the backend generates an ephemeral 256-bit cryptographically secure code (`POST /auth/desktop-handoff/issue`).
3. **Zero Token Leakage in Deep Links:** The browser redirects back via the OS protocol scheme: `crescendo://auth/callback?code=<60s-code>`. **No access tokens or refresh tokens ever appear in URLs, browser history, or system command-line logs.**
4. **Single-Instance Process Interception:** Uses `tauri-plugin-single-instance` to prevent duplicate app windows. When Windows or macOS invokes the custom scheme, the secondary launch is intercepted, the existing running window is focused and brought to front, and the handoff code is transferred over IPC.
5. **Atomic Code Exchange:** The desktop app exchanges the single-use code for dual JWT tokens (`POST /auth/desktop-handoff/exchange`) over TLS with device fingerprint validation.
6. **Defensive Fallback Options:** If browser protocol dialogs are blocked by enterprise policies, users can click "Copy sign-in link" (which contains zero secrets or tokens) or manually paste their 60-second code directly into the desktop app.

### Running Desktop Locally
```bash
# Prerequisites: Node.js 18+, Rust toolchain (cargo, rustc)
cd crescendo-frontend

# Run desktop app in development with HMR against local Vite dev server
npm run tauri dev

# Build native production executables and installers
npm run tauri build
```


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

- **Two-Layer Cache Architecture**: The backend serves Redis-cached responses (per-user TTLs, event-driven eviction via `@TransactionalEventListener`). The frontend adds React Query as a second, independent cache layer (the two solve different costs: backend reduces DB load, frontend eliminates redundant network round-trips on navigation).
- **Correct staleTime semantics**: The workflow list uses a short `staleTime` (30s) for freshness on re-visits. The open canvas uses `staleTime: Infinity` (no background refetch mid-edit), with `refetchOnWindowFocus: 'always'` as the intentional safety net.
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

### 21. Cryptographic Erasure (Crypto-Shredding) & The 9-Phase Purge Cascade

- **The Write-Ahead Log (WAL) & Immutable Backup Paradox**: In relational databases (PostgreSQL), executing SQL `DELETE` or soft deletes (`is_deleted=true`) marks rows as dead in heap pages and appends `DELETE` records to the Write-Ahead Log (`pg_wal/`). WAL segments are streamed to cold object storage (S3) for Point-In-Time-Recovery (PITR), and daily compressed database dumps are immutable (WORM storage). Standard deletes cannot scrub historical backups, leaving user plaintext credentials and OAuth tokens exposed in backups for 30–90 days in direct violation of GDPR Article 17 ("Right to Erasure").
- **Two-Tier Envelope Crypto-Shredding**: Sensitive user credentials and integration tokens are encrypted using a per-user Data Encryption Key (DEK). Each DEK is encrypted under a Master Key Encryption Key (KEK) and stored in `user_encryption_key`. On account deletion, `CryptoShreddingService.shredUserKey(userId)` permanently purges the user's DEK. Without the 256-bit AES key, all residual ciphertext across active tables, historical WAL segments, and cold S3 backups instantly becomes pure cryptographic entropy ($2^{256}$ keyspace), achieving permanent, irreversible compliance across immutable backup chains in $O(1)$ time without modifying backups.
- **Strict 9-Phase Deletion Cascade**: To prevent foreign key constraint violations (`fk_workflow_user`, `fk_connection_user`), active poller race conditions, and orphaned cloud storage blobs, `User_commandService.deleteUserAccount()` executes in strict reverse-dependency order:
  1. *Poller Deactivation*: Cancels in-memory `ScheduledFuture` tasks in `PollingTriggerScheduler`.
  2. *Workflow Purge*: Traverses and purges step execution logs $\to$ step runs $\to$ steps $\to$ parent workflows.
  3. *Connections Purge*: Removes third-party OAuth credentials and integration secrets.
  4. *Physical Blob Storage Erasure*: Physically deletes files from MinIO / S3 / local disk to prevent storage leaks, then deletes `uploaded_files` metadata rows.
  5. *Authentication & Passkeys*: Cleans up WebAuthn / FIDO2 public keys (`PasskeyCredential_commandRepository`).
  6. *Developer Infrastructure*: Revokes programmatic API keys and custom domains.
  7. *Marketing & Audience*: Purges email templates, audience contacts, and broadcasts.
  8. *Cryptographic Shredding*: Permanently destroys the per-user DEK from `user_encryption_key`.
  9. *Principal Record Purge*: Deletes the root `User_command` account entity safely without constraint deadlocks.

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

### Production Deployment

```bash
cp .env.example .env
docker compose up -d --build
```

## Testing strategy & zero-credential verification

A common challenge in integration platforms is testing hundreds of third-party actions without requiring developers to manage dozens of external developer accounts, live API keys, or flaky OAuth refresh tokens in CI.

Crescendo utilizes a **4-Layer Zero-Credential Verification Strategy**:

1. **Universal Catalog Contracts (`CatalogContractTest`, `OperationTestContractFactoryTest`)**: All 114 application integrations and 868 action mappings are scanned and audited in-memory in ~5.4 seconds (with pure assertion execution taking ~225ms across all 6 test cases once reflection classes are loaded in `@BeforeAll`). Asserts structural catalog integrity: unique app/action keys, parameter schema types, test policy assignments (`READ_TARGET`, `READ_SAMPLE`, `LOCAL_SIMULATION`), Java `@ActionMapping` handler registration parity, and valid JSON schema formatting without network connectivity or API tokens. *(Note: Runtime third-party HTTP URL routing and remote payload exchange are verified separately via Layer 3 HTTP mock seams).*
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
- **Race-Condition & Double-Execution Defense:** To guarantee protection against silent double-execution defects (where two simultaneous workers both assume lock acquisition and silently return HTTP 200 without raising server exceptions), Crescendo implements a dual-layer verification strategy:
  - **In-Memory JVM Concurrency Suite:** Automated integration tests (`DistributedLockServiceIntegrationTest`) fire 100 synchronized Virtual Threads directly in RAM across a `CountDownLatch` starting barrier, asserting exactly 1 worker acquires the lock and 99 fail cleanly.
  - **3-Tier Container Benchmark Suite (`performance-tests/`):** Standalone k6 configurations test peak Read queries, transactional Write bursts, and simultaneous race conditions via simple Docker execution commands without local software installation.

## What makes this project different from a basic CRUD app

- Asynchronous queue-driven execution engine
- Reliability primitives (outbox, lock ownership, manual ACK, DLQ, PEL reclaim)
- Event and workflow orchestration mindset
- Integration-heavy platform surface
- Product-oriented architecture choices for real-user scenarios

## Open source and contributions

Crescendo is designed and built to be developer-extensible and open-source friendly.

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

## Current status & continuous improvement

Crescendo is completed and running in production as a robust automation and transactional communication platform. With the version 1.0 foundation established, ongoing work focuses on continuous improvements: adding new catalog integrations and triggers, tuning system performance, and shipping incremental feature updates.

## Official Links & Resources

| Resource | URL | Description |
|---|---|---|
| **Live Web Platform** | [https://app.crescendo.run](https://app.crescendo.run) | Primary web application and workflow builder |
| **Documentation Portal** | [https://app.crescendo.run/docs](https://app.crescendo.run/docs) | Complete user and developer documentation guides |
| **Public API Reference** | [https://app.crescendo.run/docs/public-api](https://app.crescendo.run/docs/public-api) | REST endpoints, authentication, and headers |
| **API Governance & Idempotency** | [https://app.crescendo.run/docs/api-governance](https://app.crescendo.run/docs/api-governance) | Idempotency keys, rate limits, and error envelopes |
| **Interactive OpenAPI Specs** | [https://app.crescendo.run/docs/api/workflows](https://app.crescendo.run/docs/api/workflows) | In-browser OpenAPI v3 testing suite |
| **Node.js / TypeScript SDK** | [https://app.crescendo.run/docs/sdk-node](https://app.crescendo.run/docs/sdk-node) | Official `@crescendo/email` SDK documentation |
| **Python SDK** | [https://app.crescendo.run/docs/sdk-python](https://app.crescendo.run/docs/sdk-python) | Official `crescendo-sdk-python` documentation |
| **Multi-Language SDKs** | [https://app.crescendo.run/docs/sdk-multi-language](https://app.crescendo.run/docs/sdk-multi-language) | Java, Go, Rust, C#, PHP, Ruby & CLI documentation |
| **Universal SDK Repository** | [https://github.com/AnkitArsh19/crescendo-sdk](https://github.com/AnkitArsh19/crescendo-sdk) | Dedicated GitHub repository for client libraries |
| **Desktop Releases (v1.0.3)** | [GitHub Releases](https://github.com/AnkitArsh19/crescendo/releases/tag/v1.0.3) | Native packages for Windows (`.exe`, `.msi`), macOS (`.dmg`), and Linux (`.AppImage`, `.deb`, `.rpm`) |
| **Privacy Policy** | [https://app.crescendo.run/privacy](https://app.crescendo.run/privacy) | Data privacy, cookies, retention, and security |
| **Terms of Service** | [https://app.crescendo.run/terms](https://app.crescendo.run/terms) | Platform usage terms and service agreements |
