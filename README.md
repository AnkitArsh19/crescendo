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
- Zustand
- Axios
- React Hook Form + Zod
- Framer Motion
- React Icons
- XYFlow/React (node/flow style workflow UI)
- ESLint 9

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

This email system is part of the core platform roadmap, not an afterthought.

### Natural Language Workflow Builder (AI-ML)

Crescendo features a robust natural language-to-workflow pipeline (`crescendo-aiml`) designed to translate conversational user intents into fully executable workflows.

Rather than relying on a fragile single-shot LLM call, the AI-ML service implements a sophisticated multi-stage resolution architecture:

1. **Intent Classification & Clarification:** Rapid initial processing to determine if a prompt is actionable or requires clarifying questions back to the user.
2. **App & Action Resolution:** The model (e.g. LLaMA-3.3-70B) resolves the user's intent against Crescendo's live, dynamic app catalog, picking the exact trigger and actions needed.
3. **Configuration Inference:** Per-step configuration generation mapped precisely to the required schemas for the chosen actions.
4. **Deterministic Catalog Validation:** A pure-Python validation layer ensures the generated workflow strictly conforms to the backend's known catalog (`appKeys`, `actionKeys`), completely eliminating integration hallucinations before they reach the execution engine.
5. **Prompt Injection Defense:** Strict XML delimiting of user input to protect the system prompt and maintain workflow generation integrity.
6. **Graceful Error Propagation:** Robust failure boundaries that ensure AI limitations or ambiguities fail gracefully and surface back to the user interface appropriately.

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

### 12. Directed Rooted Tree execution

- Workflow execution modeled as a rooted tree to support branching logic (e.g. If, Switch conditions)
- Recursive sequence execution that isolates unexecuted branch state
- Fine-grained suspend and resume execution that maintains sub-branch execution context

### 13. Native Postgres Search & Async Batched Rollups

- Eliminated the need for Elasticsearch or Datadog by implementing high-performance search and metrics natively.
- **Async Rollups**: Solved heavy write-throughput and hot-row contention for time-series data using a Redis Stream consumer that flushes batched metrics every 5 seconds.
- **Postgres Search**: Utilized `tsvector` and `pg_trgm` extensions to achieve highly efficient, relevance-ranked full-text search across millions of logs.
- Why Postgres serves the purpose: No data synchronization delays, no split-brain schema issues, and significant operational simplicity compared to managing a separate ELK stack.

## Reliability and production-style concerns addressed

- Duplicate publish/race prevention with pessimistic locking on outbox reads
- Cache eviction strategy improved from blanket eviction to targeted invalidation
- Reduced sensitive token leakage in non-local contexts
- Stream listener container health checks and restart logic
- Recovery for stuck pending execution messages
- Heartbeat extension for lock TTL during long workflows

## Local development

Typical setup:

1. Start dependencies (PostgreSQL, Redis) via Docker Compose
2. Run backend via `./mvnw spring-boot:run` from `crescendo-backend`
3. Run frontend via `npm install && npm run dev` from `crescendo-frontend`

Containerized option:

- Use the provided compose files/examples to run backend + infra together

OAuth callback testing and external webhook testing:

- Local development commonly runs through Cloudflare Tunnel to provide a public HTTPS domain.
- Active testing domain: app.crescendo.run
- This enables realistic OAuth redirect URI testing and end-to-end webhook/provider callback flows.

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

## Current direction

Crescendo is being shaped as a practical automation and transactional communication platform with strong engineering foundations, where learning and real-world product quality are both first-class goals.
