# Crescendo Backend Module

The backend engine for Crescendo is an enterprise-grade Java 25 / Spring Boot platform built to drive high-concurrency automated workflow execution, dynamic application catalog integration, and real-time email orchestration. Built on a Command Query Responsibility Segregation (CQRS) design pattern, the engine utilizes Java Virtual Threads, Redis Streams, and PostgreSQL for robust, non-blocking asynchronous data pipelines.

## Architectural Foundations & Concurrency

- **CQRS Database Isolation**: Maintains isolated datasource configurations for write operations (`crescendo_command` datasource) and read operations (`crescendo_query` datasource). This segregation ensures intensive analytical reporting or dashboard reads never degrade the transaction throughput of live workflow executions.
- **Java Virtual Thread Concurrency**: Global virtual thread execution is active across all web request handlers (`spring.threads.virtual.enabled=true`), alongside custom structured Virtual Thread factories (`Thread.ofVirtual().factory()`) dedicated to background task schedulers and Redis queue consumers.
- **Strict OSIV Boundary Enforcement**: Open-Session-in-View (OSIV) is explicitly disabled (`spring.jpa.open-in-view=false`) across all environments. This ensures HikariCP database connections are checked out strictly during atomic service layer transactions and released immediately before commencing slow external network operations or rendering JSON responses.
- **Distributed Redis Locking**: Employs distributed synchronization locks (`DistributedLockService`) over Redis to eliminate concurrent execution conditions when multiple backend node instances poll scheduled triggers or evaluate time-sensitive email broadcasting campaigns.
- **Observability & Telemetry**: Exposes operational Spring Boot Actuator endpoints and Micrometer Prometheus metrics (`/actuator/prometheus`) for containerized Grafana performance visualization and cluster monitoring.

## Comprehensive App Catalog & Execution Engine

Crescendo hosts a self-contained, native application catalog featuring **114 built-in application integrations** organized under `com.crescendo.apps`. Each app provides structured action handlers and triggers fully aligned with OpenAPI configuration schemas:
- **Universal Authentication Support**: Handles OAuth 2.0 authorization code flows, static API keys, Bearer token injection, and user-provided Bring Your Own Key (BYOK) configurations across all supported external application integrations.
- **Dynamic Resource Providers**: Dedicated dropdown resource providers fetch user-specific dynamic selections (e.g., retrieving lists of channels, spreadsheets, repositories, or playlists) via standardized execution interfaces without embedding UI API formatting logic in the frontend.
- **Built-in Execution Core Modules**:
  - **HTTP Request Action (`http-request`)**: Executes real-time outbound HTTP requests supporting custom headers, dynamic authentication bearer tokens, and parameterized JSON payload publishing.
  - **Relational Database Execution (`postgresql`, `mysql`)**: Enables direct SQL script execution, table deletions, insertions, updates, and upsert operations against external databases.
  - **Branching Logic Nodes (`logic:if`, `logic:switch`)**: Evaluates real-time expressions, regex matching, numerical comparison, and multi-condition grouping (AND/OR combinators) to dynamically route workflow execution along multiple branch paths (`output_0`, `output_1`, `output_2`).
  - **Scheduling & Timing Nodes (`schedule`, `wait`)**: Provides accurate Cron string expression scheduling (`ScheduleTriggerPoller`) and parameterized delay operations during workflow runs.

## Real-Time Workflow Orchestration & Data Passing

- **Redis Stream Queue Processing**: Workflow step execution requests are published to reliable Redis Stream data structures and consumed asynchronously by `ExecutionQueueConsumer` worker threads, preventing memory overflow under spike traffic.
- **Expression Evaluation Pipeline**: The core variable resolver engine (`WorkflowExpressionResolver`) replaces upstream data reference placeholders (`{{steps.step_order.field_name}}`) in real time, supporting safe fallback handling for missing array keys or null properties during multi-step execution chains.
- **Resilient Retry Mechanics**: Incorporates automated backoff calculation algorithms and circuit breaking to gracefully handle rate-limit rejections or network latency when connecting to external third-party API servers.

## Integrated Email & DNS Verification Service

The backend incorporates an independent email marketing and transactional delivery system under `com.crescendo.emailservice`:
- **Transactional & Audience Delivery**: Manages customer contact audiences, scheduled marketing campaigns, and real-time operational transactional messaging.
- **Cryptographic DNS Authentication**: Automatically evaluates domain DNS configuration records (`DnsVerificationService`), verifying valid Sender Policy Framework (SPF), DomainKeys Identified Mail (DKIM) TXT strings, and DMARC compliance to ensure inbox placement.
- **Telemetry Injection & Bounce Webhooks**: Dynamically injects open-tracking image pixels (`/t/o/{emailId}`) and link click wrapper paths into outgoing HTML email template blocks while listening for SMTP delivery bounce event webhooks to maintain domain sender reputation.

## Quality Assurance & Zero-Credential Testing Strategy

Crescendo implements a rigorous automated QA strategy that requires **no live external developer accounts, zero secret keys in local configuration files, and no internet connectivity to run**:
- **Universal Catalog Contract Suite (`CatalogContractTest`)**: Audits all 114 application integrations and action mappings in memory in under two seconds. Confirms exact parity between advertised UI schemas and compiled Java execution handlers, eliminating runtime missing-handler crashes.
- **Local HTTP Seam Assurance (`ResourceProviderHttpContractTest`)**: Uses lightweight embedded local web servers inside Java to simulate third-party vendor APIs, asserting correct OAuth authorization headers and dynamic UI dropdown JSON parsing without external HTTP network dependency.
- **Direct Execution Unit Verification**: Extensive unit and execution contract suites cover core system behaviors:
  - `LogicHandlersUnitTest`: Evaluates If/Switch logic branch calculation and operator comparisons directly in memory.
  - `HttpHandlersExecutionTest`: Verifies actual HTTP client request construction and response code parsing against embedded test server fixtures.
  - `ScheduleTriggerPollerTest`: Confirms exact interval timestamps and Cron string evaluation mechanics.
  - `EmailServiceProtocolIntegrationTest`: Validates transactional HTML formatting and tracking URL injection syntax.

## Directory & Package Topology

The backend application is organized cleanly under `src/main/java/com/crescendo`. Below is the detailed breakdown of all parent packages and specialized sub-packages illustrating what each domain handles:

```text
crescendo-backend/src/main/java/com/crescendo/
├── CrescendoApplication.java    # Spring Boot primary bootstrap root and virtual thread flag initializer
├── admin/                       # Platform-level management services, operational monitoring diagnostics, and tenant administration endpoints
├── ai/                          # AI/ML microservice HTTP integration adapters and natural language automation prompt bindings
├── aop/                         # Aspect-Oriented Programming (AOP) cross-cutting advisors for telemetry metrics, transaction timing, and exception interception
├── app/                         # Core integration domain models, common catalog descriptors, and application metadata annotations
├── apps/                        # Complete repository of all 114 native integration modules (e.g., mysql, google-sheets, slack, spotify, jokeapi, toggl, logic, wait)
├── auth/                        # User onboarding authentication flows, login authorization verification, and multi-tenant session token negotiation
├── config/                      # Spring Boot global runtime configurations (CQRS datasources, WebSecurity, CORS rules, Redis stream connection pools, Virtual Threads)
├── connections/                 # Encrypted credential vaults storing third-party OAuth access/refresh token pairs, static BYOK API keys, and connection validation trackers
├── emailservice/                # Complete enterprise transactional email delivery and marketing campaign broadcasting infrastructure:
│   ├── apikey/ & provider/      # Third-party SMTP transmission provider adapters and secure email delivery API key vaults
│   ├── audience/ & broadcast/   # Customer contact directory management, audience segment grouping, and scheduled campaign broadcasters
│   ├── dmarc/, domain/          # Automated cryptographic DNS authentication engines evaluating SPF, DKIM TXT strings, and DMARC policy compliance
│   ├── emailtemplate/           # Dynamic transactional and marketing HTML email template rendering engines and block compilers
│   ├── spam/, suppression/      # Automated bounce suppression list handlers, complaint registries, and anti-spam protection rule checkers
│   └── tracking/, webhook/      # Open-tracking image pixel injection (/t/o/{emailId}), click routing wrapping telemetry, and SMTP bounce delivery event listeners
├── enums/                       # Global immutable domain enumerations covering workflow run execution states, step status flags, authentication roles, and catalog scopes
├── execution/                   # High-concurrency workflow automation runtime and step processing engine:
│   ├── action/ & trigger/       # Application action execution dispatchers, event listeners, and scheduled trigger polling routers
│   ├── condition/ & engine/     # Branch logical evaluation processors (If/Switch operators) and central Directed Acyclic Graph (DAG) execution orchestrator
│   ├── expression/              # Dynamic variable substitution engine (WorkflowExpressionResolver) for resolving upstream step references ({{steps.order.field}})
│   ├── queue/ & resource/       # Execution job scheduling buffers and dynamic third-party integration catalog dropdown resource providers
│   └── suspension/, test/       # Execution suspension/resume controllers for long-running Wait timers or human approval steps, alongside local execution mock helpers
├── logbook/                     # Immutable activity tracking, operational audit trails, user workflow modification logs, and regulatory compliance records
├── publicapi/                   # Publicly accessible Developer REST API endpoints, automated OpenAPI specification generator, and external SDK execution routing controllers
├── security/                    # Tenant perimeter defense, request sanitization, and cryptographic identity verification boundaries:
│   ├── access/, oauth/          # OAuth Bearer token authorization filters, token reuse detection defenses, and fine-grained access permissions
│   ├── alerts/, error/          # Real-time Geo-IP access anomaly detection services and centralized security exception response formatting
│   └── mfa/, webauthn/          # Biometric WebAuthn passkey registration endpoints, passwordless authentication routines, and Multi-Factor Authentication verifiers
├── settings/                    # Workspace customization controllers, team membership permission toggles, and notification routing preferences
├── shared/                      # Shared Domain-Driven Design (DDD) architectural primitives and foundational infrastructure:
│   ├── domain/, util/           # Common domain events, uniform business exception hierarchies, and core domain value records
│   └── infrastructure/stream/   # Redis Streams high-throughput asynchronous job publishers and clustered execution multi-threaded consumers (ExecutionQueueConsumer)
├── steps/                       # Workflow step canvas entities, configuration schema mapping validation, and step sequencing attribute repositories
├── storage/                     # Binary object storage adapters, workspace media asset persistence, and email file attachment storage managers
├── user/                        # Tenant user account repository, workspace membership mapping, and profile state descriptors
├── utils/                       # Universal static helper utilities covering JSON formatting, timestamp conversion, cryptographic string encoding, and network helpers
├── web/                         # Standard Spring MVC REST routing controllers, global exception advice handlers, and standardized JSON HTTP response builders
├── webhook/                     # Third-party incoming webhook receptors, dynamic callback URL endpoints, payload cryptographic signature verification, and trigger dispatchers
└── workflow/                    # Core Directed Acyclic Graph (DAG) canvas entities, version history tracking, and workflow controllers:
    ├── workflow_command/        # CQRS command write services dedicated to handling canvas creations, step modifications, and structural DAG validations
    ├── workflow_query/          # CQRS query read services optimized for fast canvas rendering, workflow dashboard list loading, and historical execution lookups
    └── domain_event/            # Real-time workflow state transition event emitters and Server-Sent Events (SSE) live streaming broadcast controllers (WorkflowSseController)
```

## Local Setup & Development Commands

```bash
# 1. Navigate into the backend module workspace
cd crescendo-backend

# 2. Recompile codebase and execute the complete zero-credential test suite
./mvnw clean test

# 3. Execute targeted unit and contract test suites individually
./mvnw test -Dtest="CatalogContractTest,LogicHandlersUnitTest,HttpHandlersExecutionTest,PostgreSqlHandlersTest,ScheduleTriggerPollerTest,WorkflowExpressionResolverTest,EmailServiceProtocolIntegrationTest"

# 4. Launch Spring Boot application locally (Connects to localhost PostgreSQL & Redis instances)
./mvnw spring-boot:run
```

For custom database ports or local credential configuration adjustments, consult `src/main/resources/application.properties.example` and `src/main/resources/application-local.properties.example`.
