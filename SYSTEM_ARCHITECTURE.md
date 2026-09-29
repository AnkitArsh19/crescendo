# Crescendo Platform: Complete System Architecture & Engineering Blueprint

This document provides the authoritative, component-by-component architectural specification of the **Crescendo** workflow automation platform. It reflects the exact multi-tier distributed topology, cryptographic protocols, token bucket rate limiting, ReAct AI execution loops, transactional email pipelines, and fault-tolerant execution state machines validated by our Archify architecture models.

> **Architecture Suite**: High-resolution visual diagrams and formal declarative specifications are maintained under:  
> **[architecture/README.md](file:///e:/crescendo-backend/architecture/README.md)**

---

## Architecture Navigation Index

1. [Master Platform Distributed Architecture](#1-master-platform-distributed-architecture)
2. [Perimeter Admission Control: Redis Lua Token Bucket Rate Limiter](#2-perimeter-admission-control-redis-lua-token-bucket-rate-limiter)
3. [Authentication & Session Security: OAuth 2.0 PKCE & Refresh Token Rotation](#3-authentication--session-security-oauth-20-pkce--refresh-token-rotation)
4. [AI Microservice: Autonomous AI ReAct Agent Loop](#4-ai-microservice-autonomous-ai-react-agent-loop)
5. [Transactional Messaging: Email Delivery Engine & Telemetry Feedback Loop](#5-transactional-messaging-email-delivery-engine--telemetry-feedback-loop)
6. [Fault-Tolerant Execution: Workflow Engine State Machine Lifecycle](#6-fault-tolerant-execution-workflow-engine-state-machine-lifecycle)
7. [Subsystem Deep Dives & Operational Lifecycles](#7-subsystem-deep-dives--operational-lifecycles)
8. [Hardware & Resource Budget (4 GiB Single-VPS Profile)](#8-hardware--resource-budget-4-gib-single-vps-profile)

---

## 1. Master Platform Distributed Architecture

The Crescendo platform architecture establishes strict security perimeters, multi-tenant isolation, and zero-trust communication across five tiers:

![Crescendo Platform Distributed Architecture](architecture/assets/dark/platform.png)

*Visual Diagram*: [Dark Mode](architecture/assets/dark/platform.png) · [Light Mode](architecture/assets/light/platform.png) | *JSON Specification*: [platform.architecture.json](architecture/specs/platform.architecture.json)

### System Tier Breakdown

```
+-----------------------------------------------------------------------------------------------------------------------------------------+
|                                                      CLIENTS & INGRESS TIER                                                             |
|  [ Web Browser SPA ]      [ Tauri Native Desktop ]      [ Universal SDKs ]      [ Webhook Emitters ]                                    |
|   (React 19 / Vite)        (Rust / RFC 8252 Handoff)     (Node/Python/Go)       (GitHub, Stripe, etc.)                                  |
|           |                           |                         |                        |                                              |
|           +---------------------------+-------------------------+------------------------+                                              |
|                                       | (HTTPS / TLS)                                                                                   |
|                                       v                                                                                                 |
|                        [ Cloudflare Edge CDN & Tunnel ] <---- Zero Public Host Ports Exposed                                            |
+---------------------------------------+-------------------------------------------------------------------------------------------------+
                                        | (Internal Docker Bridge Network)
                                        v
+-----------------------------------------------------------------------------------------------------------------------------------------+
|                                               CORE SERVICES (Java 25 Spring Boot)                                                       |
|                                                                                                                                         |
|  +---------------------------+   +---------------------------+   +---------------------------+   +-------------------------------+  |
|  |    Ingress Controllers    |   | Workflow Execution Engine |   | Transactional Email Engine|   |   AI Rate Limiter & Queue     |  |
|  |  - Auth & WebAuthn / Pass |   |  - DAG Edge-State Router  |   |  - 5-Layer ESP Pipeline   |   |  - 14 RPM / 480 RPD Gate      |  |
|  |  - Public API (/api/v1/*) |   |  - Catalog Registry (114+)|   |  - Daily Warming Engine   |   |  - 30s Redis Holding Queue    |  |
|  |  - Webhook Ingest (HMAC)  |   |  - Native REST Fallback   |   |  - RFC 8058 Unsubscribe   |   |  - 3s Downstream 429 Backoff  |  |
|  +-------------+-------------+   +-------------+-------------+   +-------------+-------------+   +---------------+---------------+  |
|                |                               |                               |                                 |                      |
|                | Write Intent & Events         | Dequeue Step & Lock           | Send Tasks                      | Evaluate Quota       |
|                v                               v                               v                                 v                      |
|  +-----------------------------------------------------------------------------------------------------------------------------------+  |
|  | Schedulers & Daemons (Virtual Threads): OutboxPublisher (5s) | PollingScheduler (120s) | PEL Reaper (>60s) | DomainWarming (Daily)   |  |
|  +---------------------------------------------+-----------------------------------------------------------------+-------------------+  |
+------------------------------------------------|-----------------------------------------------------------------|----------------------+
                                                 |                                                                 |
                   Publish Outbox Events         | Consume Execution Steps (Manual ACK)                            | ReAct Invocation
                   v                             v                                                                 v
+------------------------------------------------+--------------+  +-----------------------------------------------+----------------------+
|             MESSAGING (Redis 7 Streams)                       |  |        AI / ML MICROSERVICE (crescendo-aiml)                         |
|                                                               |  |                                                                      |
|  [ crescendo:queue:execution ] <--- Workflow Steps to Execute |  |  +----------------------------+   +-------------------------------+  |
|  [ crescendo:queue:email ]     <--- Email Send Jobs           |  |  | Autonomous ReAct AI Agent  |   | Natural Language DAG Builder  |  |
|  [ crescendo:events:* ]        <--- Domain Events Fanout      |  |  |  - Tool Calling Loop       |   |  - Intent Classification      |  |
|  [ crescendo:dlq ]             <--- Poisoned Retried Messages |  |  |  - XML Boundary Protection |   |  - Catalog Constraint Check   |  |
|                                                               |  |  +--------------+-------------+   +---------------+---------------+  |
|  Consumer Group: crescendo-backend (Manual ACK Required)      |  |                 |                                 |                  |
+--------------------------------+------------------------------+  |                 +----------------+----------------+                  |
                                 |                                 |                                  v                                   |
                                 |                                 |               [ Redis Conversational Checkpointer ]                  |
                                 |                                 |                  (Multi-turn Memory via Redis)                       |
                                 |                                 +----------------------------------+-----------------------------------+
                                 |                                                                    |
                                 |                                                                    v
                                 |                                                  [ LLM APIs: Gemini 3.5 / GPT-OSS ]
                                 v
+-----------------------------------------------------------------------------------------------------------------------------------------+
|                                                 CQRS DATA STORES & PERSISTENCE                                                          |
|                                                                                                                                         |
|  [ Command DB (PostgreSQL 16) ]      [ Query DB (PostgreSQL 16) ]        [ Redis 7 Cache & Locks ]       [ File Storage ]               |
|   - Write side, strict foreign keys   - Denormalized read models          - Atomic Lua Distributed Locks  - Local NVMe / S3 / R2        |
|   - Transactional Outbox table        - Fast index-only scans for UI      - Token Ownership UUIDs         - Uploaded File Command       |
|   - Per-User Encryption Keys (DEK)    - Zero lock contention with writes  - Rate Limit Key Buckets        - Presigned download URLs     |
|   - 16 MB WAL Segments, 8 Conns       - 8 Hikari Connection Pool          - In-memory AOF persistence     - FileStorageService          |
+-----------------------------------------------------------------------------------------------------------------------------------------+
```

1. **Client & Ingress Perimeter**:
   - Web application built with React 19 / Vite.
   - Native desktop application compiled with Tauri v2 (Rust), supporting deep-link authorization handoffs ([RFC 8252](https://datatracker.ietf.org/doc/html/rfc8252)).
   - Public APIs fronted by Cloudflare edge caching, DDoS mitigation, and Cloudflare Tunnel zero-trust origin cloaking.
2. **Security & Admission Control Tier**:
   - Edge security enforced at [`RateLimitFilter.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/security/RateLimitFilter.java) via Redis Lua token buckets.
   - Stateless JWT authentication ([`JwtService.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/security/JwtService.java)) paired with stateful refresh token cookie verification ([`RefreshTokenCookieService.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/security/RefreshTokenCookieService.java)).
3. **Core Backend Application Tier**:
   - CQRS write-side services ([`Workflow_commandService.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/workflow/workflow_command/Workflow_commandService.java), [`User_commandService.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/user/user_command/User_commandService.java)) execute transactional writes and write domain events to the outbox.
   - Read-side services ([`Workflow_queryService.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/workflow/workflow_query/Workflow_queryService.java)) query denormalized PostgreSQL read projections with zero write-lock contention.
   - DAG workflow execution orchestrated by [`WorkflowExecutionEngine.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/execution/engine/WorkflowExecutionEngine.java).
4. **Data & Polyglot Persistence Tier**:
   - **PostgreSQL 16**: Relational schema, ACID transactions, row-level tenant security, and transactional outbox.
   - **Redis 7 Cluster**: Stream queues (`crescendo:queue:execution`, `crescendo:queue:email`), distributed lock leases with background heartbeats, and token buckets.
   - **NVMe / S3 / Cloudflare R2**: Presigned file uploads managed by [`FileStorageService.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/service/FileStorageService.java).
5. **AI Microservice Tier**:
   - Standalone Python 3.12 FastAPI microservice (`crescendo-aiml`) running LangGraph and autonomous ReAct agents.

---

## 2. Perimeter Admission Control: Redis Lua Token Bucket Rate Limiter

Crescendo shields internal controllers and background workers from request bursts and tenant quota exhaustion through an in-memory token bucket rate limiter evaluated directly in Redis:

![Redis Lua Token Bucket Rate Limiter](architecture/assets/dark/rate-limiter.png)

*Visual Diagram*: [Dark Mode](architecture/assets/dark/rate-limiter.png) · [Light Mode](architecture/assets/light/rate-limiter.png) | *JSON Specification*: [rate-limiter.sequence.json](architecture/specs/rate-limiter.sequence.json)

### Mathematical Invariants & Lua Script Mechanics
- **Source**: [`RateLimitingService.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/security/RateLimitingService.java)
- **Perimeter Filter**: [`RateLimitFilter.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/security/RateLimitFilter.java)
- **Configuration**: [`RateLimitConfig.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/config/RateLimitConfig.java)

1. **Atomic Server-Side Evaluation**:
   The rate limit check executes inside a single Redis script preloaded via `EVALSHA`. Redis's single-threaded event loop guarantees zero race conditions without distributed lock overhead.
2. **Clock Skew Prevention**:
   The script invokes Redis's native `TIME` command to obtain `[seconds, microseconds]`. Elapsed time is calculated purely against Redis server time, eliminating clock skew across multi-node backend instances.
3. **Integer Millitoken Precision**:
   Token math is represented in integer millitokens ($1\text{ token} = 1{,}000\text{ millitokens}$). This eliminates IEEE 754 floating-point rounding errors during continuous fractional token refill:
   $$\text{refill} = \min\left(\text{burst} \times 1000, \text{current} + \Delta t \times \text{rate}\right)$$
4. **Admission & Standard Headers**:
   When $\text{current} \ge 1{,}000\text{ millitokens}$, 1,000 millitokens are deducted and the HTTP request proceeds with RFC-standard headers:
   - `X-RateLimit-Limit`: Maximum bucket capacity.
   - `X-RateLimit-Remaining`: Floor integer tokens remaining.
   - `X-RateLimit-Reset`: UNIX timestamp in seconds when the bucket completely refills.
5. **Perimeter Short-Circuit on Burst Rejection**:
   When $\text{current} < 1{,}000\text{ millitokens}$, the request is rejected immediately at the filter with HTTP `429 Too Many Requests` and a computed `Retry-After: <seconds>` header, preventing resource consumption by downstream Spring controllers.

---

## 3. Authentication & Session Security: OAuth 2.0 PKCE & Refresh Token Rotation

Crescendo implements zero-trust session management using Proof Key for Code Exchange ([RFC 7636](https://datatracker.ietf.org/doc/html/rfc7636)), stateful token families, single-use refresh token rotation, and automated compromise containment:

![OAuth 2.0 PKCE & Refresh Token Rotation](architecture/assets/dark/oauth-pkce-reuse.png)

*Visual Diagram*: [Dark Mode](architecture/assets/dark/oauth-pkce-reuse.png) · [Light Mode](architecture/assets/light/oauth-pkce-reuse.png) | *JSON Specification*: [oauth-pkce-reuse.sequence.json](architecture/specs/oauth-pkce-reuse.sequence.json)

### Cryptographic Protocols & Security Invariants
- **Session Controller**: [`SessionController.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/security/SessionController.java)
- **Token Cryptography**: [`JwtService.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/security/JwtService.java)
- **Cookie Security**: [`RefreshTokenCookieService.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/security/RefreshTokenCookieService.java)

1. **SHA-256 Code Challenge Verification**:
   The client creates a high-entropy `code_verifier` ($[43..128]$ unreserved characters) and computes `code_challenge = BASE64URL-ENCODE(SHA256(code_verifier))`. Upon code exchange, the backend verifies the SHA-256 hash before issuing tokens, defending against authorization code interception in public clients.
2. **Dual-Token Architecture**:
   - **Access Token**: Stateless, short-lived (15 minutes) JWT containing user permissions and tenant UUIDs.
   - **Refresh Token**: Stateful, cryptographically random token stored in an `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth` cookie, inaccessible to browser JavaScript.
3. **Atomic Single-Use Token Rotation**:
   Every call to `POST /api/v1/auth/refresh` invalidates the submitted refresh token and generates a new token linked to the existing session family, updating `last_used_at` and incrementing the family generation counter.
4. **Token Family Compromise Detection & Revocation**:
   If a compromised refresh token ($R_1$) is replayed after it has already been rotated ($R_2$ active), [`SessionController.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/security/SessionController.java) flags an active token theft event. The engine executes cascading revocation, destroying the entire session family tree in the database and revoking active sessions across both the legitimate user and the attacker.

---

## 4. AI Microservice: Autonomous AI ReAct Agent Loop

The autonomous agent subsystem in `crescendo-aiml` implements a LangGraph-based ReAct (Reason $\rightarrow$ Act $\rightarrow$ Observe) loop connecting LLM reasoning to Crescendo's 50+ third-party catalog integrations:

![Autonomous AI ReAct Agent Loop](architecture/assets/dark/react-agent.png)

*Visual Diagram*: [Dark Mode](architecture/assets/dark/react-agent.png) · [Light Mode](architecture/assets/light/react-agent.png) | *JSON Specification*: [react-agent.workflow.json](architecture/specs/react-agent.workflow.json)

### Agent Orchestration & Execution Flow
- **FastAPI Agent Service**: [`agent_service.py`](file:///e:/crescendo-backend/crescendo-aiml/app/services/agent_service.py)
- **Spring AI Bridge**: [`AgentExecutionService.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/execution/agent/AgentExecutionService.java)
- **Sub-Workflow Tool Runner**: [`SubWorkflowToolRunner.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/execution/agent/SubWorkflowToolRunner.java)

1. **System Prompt & Tool Schema Assembly**:
   The prompt formulation stage injects conversational state, user permissions, and strict JSON schemas derived from Crescendo's dynamic app catalog (`Slack`, `GitHub`, `Notion`, `Jira`, `Gmail`, etc.).
2. **Multi-Model Inference & Reasoning**:
   The agent queries foundational models (Google Gemini 1.5/2.0, Anthropic Claude 3.5, or OpenAI GPT-4o) using native function-calling formats, extracting structured thought reasoning and tool call payloads.
3. **Connector Execution & Three-Tier Credentials**:
   When a tool call is emitted, [`AgentExecutionService.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/execution/agent/AgentExecutionService.java) resolves authentication credentials using a three-tier hierarchy:
   $$\text{Personal OAuth/API Key} \longrightarrow \text{Platform Admin Key} \longrightarrow \text{Execution Abort}$$
4. **Observation Feedback & XML Boundary Protection**:
   Tool outputs are captured, wrapped in structured XML boundary markers (`<tool_output_content>`), and appended to the context window to prevent prompt injection from untrusted external APIs.
5. **Iteration Cap & Termination Safeguards**:
   To prevent infinite execution loops and unbounded LLM costs, the agent loop enforces a hard ceiling of 10 iterations per task. If the budget is exhausted without reaching a final answer, the loop cleanly terminates with an escalation report.

---

## 5. Transactional Messaging: Email Delivery Engine & Telemetry Feedback Loop

Crescendo incorporates an enterprise-grade transactional email delivery engine designed for high deliverability, cryptographic domain reputation governance, and automated bounce quarantine feedback:

![Transactional Email Delivery Engine](architecture/assets/dark/email-engine.png)

*Visual Diagram*: [Dark Mode](architecture/assets/dark/email-engine.png) · [Light Mode](architecture/assets/light/email-engine.png) | *JSON Specification*: [email-engine.dataflow.json](architecture/specs/email-engine.dataflow.json)

### Pipeline Stages & Feedback Architecture
- **Email Send Service**: [`EmailSendService.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/service/EmailSendService.java)
- **Stream Queue Consumer**: [`EmailQueueConsumer.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/service/EmailQueueConsumer.java)
- **Template Rendering**: [`TemplateInterpolator.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/service/TemplateInterpolator.java)
- **DNS & Cryptographic Verification**: [`DnsVerificationService.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/service/DnsVerificationService.java)
- **Webhook Ingestion**: [`EmailEventWebhookController.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/controller/EmailEventWebhookController.java)
- **Suppression Management**: [`EmailSuppressionService.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/service/EmailSuppressionService.java)

1. **Ingest & Template Rendering**:
   [`EmailSendService`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/service/EmailSendService.java) receives transactional send events, interpolates dynamic Mustache/Thymeleaf variables via [`TemplateInterpolator`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/service/TemplateInterpolator.java), sanitizes HTML content against script injection, and generates a `PENDING` audit log.
2. **Cryptographic Signing & Pre-Send Policy**:
   - Evaluates recipient eligibility against the suppression list.
   - [`DnsVerificationService`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/service/DnsVerificationService.java) conducts JNDI DNS TXT record lookups verifying SPF (`v=spf1`), DKIM (`<selector>._domainkey.<domain>`), and DMARC (`v=DMARC1`), injecting RSA-SHA256 DKIM cryptographic signatures into message headers.
3. **Stream Buffering & SMTP Relay**:
   Jobs are enqueued to Redis Stream `crescendo:queue:email`. [`EmailQueueConsumer`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/service/EmailQueueConsumer.java) claims batches under distributed locks, injects RFC 8058 `List-Unsubscribe` one-click headers, and relays Jakarta Mail `MimeMessage` payloads via Amazon SES, SendGrid, or native SMTP relays.
4. **Webhook Ingestion & HMAC Verification**:
   External ESPs deliver asynchronous delivery telemetry to `/webhooks/email-events`. [`EmailEventWebhookController`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/controller/EmailEventWebhookController.java) validates the HMAC-SHA256 `X-Webhook-Signature` before parsing payload events (`delivered`, `hard_bounce`, `soft_bounce`, `complaint`).
5. **Suppression Quarantine Feedback Loop**:
   Hard bounces and spam complaints trigger [`EmailSuppressionService`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/service/EmailSuppressionService.java), permanently quarantining the recipient address in [`EmailSuppressionRepository`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/repository/EmailSuppressionRepository.java). Subsequent outbound dispatches check this table and abort immediately, protecting sender domain reputation.

---

## 6. Fault-Tolerant Execution: Workflow Engine State Machine Lifecycle

Crescendo's DAG workflow execution engine coordinates resilient, multi-step asynchronous business processes, incorporating distributed lock leases, asynchronous suspension, automatic exponential backoff, dead-letter routing, and state-preserving retries:

![Workflow Execution Engine Lifecycle](architecture/assets/dark/workflow-lifecycle.png)

*Visual Diagram*: [Dark Mode](architecture/assets/dark/workflow-lifecycle.png) · [Light Mode](architecture/assets/light/workflow-lifecycle.png) | *JSON Specification*: [workflow-lifecycle.lifecycle.json](architecture/specs/workflow-lifecycle.lifecycle.json)

### State Machine Lifecycle Bands
- **Execution Engine**: [`WorkflowExecutionEngine.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/execution/engine/WorkflowExecutionEngine.java)
- **Queue Consumer**: [`ExecutionQueueConsumer.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/shared/infrastructure/stream/ExecutionQueueConsumer.java)
- **Suspension Coordination**: [`WorkflowSuspensionService.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/execution/suspension/WorkflowSuspensionService.java)
- **Crash Recovery Reaper**: [`WorkflowRunReaper.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/execution/queue/WorkflowRunReaper.java)
- **Resume Sweeper**: [`WorkflowResumeReaper.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/execution/queue/WorkflowResumeReaper.java)
- **Status Enumeration**: [`WorkflowRunStatus.java`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/enums/WorkflowRunStatus.java)

```
[01 / Execution Phases]
  [01 Pending] ======> [02 Dispatched] ======> [03 Running] ======> [04 Evaluating] ======> [05 Completed]
      ^                                          │     ▲               │
      │                                          │     │ (Resume)      ▼ (Transient Error)
[02 / Suspension & Recovery]                     ▼     │         [Retry Backoff] ───► [Dead-Lettered]
      │                                    [Suspended]─┘               │ (Exhausted / Poison)
      │                                          │ (Cancel)            ▼
[03 / Terminal Outcomes]                         ▼                  [Failed]
      │                                     [Cancelled]                │
      │                                                                │
      └─────────────────────── (Manual Retry Loop) ────────────────────┘
```

1. **Band 01: Execution Phases (`main`)**:
   - `01 Pending`: Run enqueued via outbox pattern to Redis Stream `crescendo:queue:execution`.
   - `02 Dispatched`: [`ExecutionQueueConsumer`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/shared/infrastructure/stream/ExecutionQueueConsumer.java) claims the message, acquires a distributed lock lease (`crescendo:lock:workflow-execution:{id}`) with a 3-minute TTL, and launches a background thread extending the lock lease every 3 minutes.
   - `03 Running`: [`WorkflowExecutionEngine`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/execution/engine/WorkflowExecutionEngine.java) traverses the step DAG, executing [`ActionHandler`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/execution/action/ActionHandler.java) instances and chaining output data into `executionState`.
   - `04 Evaluating`: Branch condition evaluators route execution along active edges (`logic:if`, `logic:switch`), skipping unselected branches and checking step success status.
   - `05 Completed`: Reached when all active DAG steps succeed; status transitions to `SUCCESS` and publishes [`WorkflowRunCompletedEvent`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/logbook/domain_event/WorkflowRunCompletedEvent.java).
2. **Band 02: Suspension & Recovery (`interruptions`)**:
   - `Suspended`: Thrown when a step raises [`SuspendExecutionException`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/execution/action/SuspendExecutionException.java) (e.g., [`WaitHandlers`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/apps/wait/WaitHandlers.java) delay, approval requests, or webhook correlation). The worker releases its thread and distributed lock.
   - `Resume Event`: Webhook events or [`WorkflowResumeReaper`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/execution/queue/WorkflowResumeReaper.java) (sweeping ready-to-resume timeouts every 10 seconds) inject the payload and transition the run back to `Running`.
   - `Retry Backoff`: Transient step errors leave the Redis stream message unacknowledged (`XACK` withheld), triggering exponential backoff redelivery.
3. **Band 03: Terminal Outcomes (`terminal`)**:
   - `Cancelled`: User requests cancellation via [`WorkflowRunService.cancelRun()`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/logbook/workflow_run/WorkflowRunService.java).
   - `Failed`: Unrecoverable errors, exhausted retries, or hanging runs swept by [`WorkflowRunReaper`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/execution/queue/WorkflowRunReaper.java) (15-minute hanging timeout).
   - `Dead-Lettered`: Poison messages failing 3 consecutive attempts are acknowledged to halt redelivery and pushed to `crescendo:dlq`.
   - **State-Preserving Manual Retry**: Users can invoke [`WorkflowRunService.retryRun()`](file:///e:/crescendo-backend/crescendo-backend/src/main/java/com/crescendo/logbook/workflow_run/WorkflowRunService.java) from `Failed`, which resets status to `Pending` while preserving accumulated `executionState`—allowing execution to skip previously completed steps and resume directly from the point of failure.

---

## 7. Subsystem Deep Dives & Operational Lifecycles

### A. The Lifecycle of a Polling Trigger
```
[ PollingTriggerScheduler ]
        │  (Runs every 120s / 2 min default: crescendo.polling.interval-ms)
        ▼
[ Fetch Credentials & Decrypt ] ──► ConnectionCredentialsCryptoService (AES-256-GCM)
        │
        ▼
[ Outbound HTTP Call to Third Party ] (e.g. GitHub: GET /repos/{owner}/{repo}/issues)
        │
        ├── No new items found ──► Advance safe cursor (now - 1m buffer) & sleep
        │
        └── New items found!
                 │
                 ▼
[ Redis SETNX Message Deduplication ] ──► Key: polling:seen:{step}:{msgId} (7-day TTL)
        │  (Skipped if already processed by another cluster node)
        ▼
[ Transaction Boundary: Command DB ]
   ├── Insert Workflow Run record (status: PENDING)
   └── Insert event to `logbook_outbox` table
                 │
                 ▼ (Commit)
[ OutboxPublisher Loop (5s / 5,000ms) ]
   └── Reads outbox batch ──► Appends to `crescendo:queue:execution` Redis Stream
```

---

### B. The Lifecycle of Account Deletion & Cryptographic Shredding
```
[ User issues DELETE /users/me ]
        │
        ▼
[ User_commandService.deleteAccount(userId) ] (Strict Cascading Order)
        │
        ├─► 1. Purge Workflows & Stop Polling Triggers (Workflow_commandService)
        │      ├── Deactivate workflows
        │      ├── Publish WorkflowDeletedEvent ──► PollingTriggerScheduler halts polling immediately
        │      └── Hard delete steps, edges, query projections, and command workflows
        │
        ├─► 2. Purge Connections & Credentials (Connections_commandService)
        │      └── Delete command credentials & query projections
        │
        ├─► 3. Purge Uploaded Files (FileStorageService)
        │      ├── Delete physical objects from S3 / disk storage key
        │      └── Delete uploaded_file_command rows
        │
        ├─► 4. Purge Email Service Resources
        │      └── Delete contacts, broadcasts, email templates, API keys, and custom domains
        │
        ├─► 5. Purge WebAuthn Passkeys
        │      └── Delete passkey credentials from passkey repository
        │
        ├─► 6. Cryptographic Erasure & Shredding (CryptoShreddingService)
        │      └── DELETE FROM user_encryption_key WHERE user_id = userId
        │          (Destroys user's 256-bit AES DEK; historic backups & WAL logs become unrecoverable)
        │
        ├─► 7. Purge MFA Security Settings & Backup Codes
        │      └── Delete user_mfa_backup_code rows first (FK constraint), then user_mfa_setting
        │
        ├─► 8. Wipe Active Sessions, Credentials & Identities
        │      ├── Revoke all active sessions (setRevokedAt = now) & delete user_session records
        │      └── Delete user_credential (password hash) and user_identity rows
        │
        └─► 9. Hard Delete User & Emit Terminal Audit Event
               ├── DELETE FROM user_command WHERE id = userId
               └── Publish UserAccountDeletedEvent
```

---

## 8. Hardware & Resource Budget (4 GiB Single-VPS Profile)

Crescendo is engineered to run reliably in resource-constrained containerized environments, operating on a single 4 GiB VPS:

| Subsystem / Container | Allocated RAM | Share | Architectural Purpose |
| :--- | :--- | :--- | :--- |
| **JVM Backend (Java 25)** | 1,018 MiB | ~25% | Spring Boot, Virtual Threads, Hikari connection pool, execution engine |
| **PostgreSQL 16 (CQRS)** | 440 MiB | ~11% | Dual-database schemas (`crescendo_command`, `crescendo_query`), WAL segments |
| **Redis 7 (Streams & Locks)** | 256 MiB | ~6% | AOF-enabled stream queues, atomic Lua token buckets, distributed locks |
| **Python AI/ML Microservice** | 350 MiB | ~8% | FastAPI, LangGraph ReAct agent loop, tool function mapping |
| **Frontend SPA (Nginx)** | 25 MiB | <1% | Serving pre-compiled React 19 / Vite bundles with gzip/brotli |
| **Prometheus TSDB** | 200 MiB | ~5% | Metrics scraping from Spring Boot `/actuator/prometheus` |
| **Cloudflare Tunnel Daemon** | 35 MiB | <1% | Outbound encrypted tunnel cloaking public ports |
| **Linux OS & Page Cache** | 1,772 MiB | ~43% | Kernel memory, disk page caching, TCP socket buffers |
| **TOTAL ALLOCATED** | **4,096 MiB** | **100%** | Production VPS Hardware Envelope |
