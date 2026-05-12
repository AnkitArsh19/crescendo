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

Crescendo also includes a transactional email subsystem being built alongside automation features, with a product direction similar to developer-first email platforms.

- API-key based email sending
- Async email queue processing via Redis streams
- Template-driven email workflows
- Domain verification model
- Provider abstraction for delivery backends
- Delivery status lifecycle tracking

This email system is part of the core platform roadmap, not an afterthought.

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
