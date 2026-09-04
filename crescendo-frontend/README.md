# Crescendo Frontend Module

The user interface for Crescendo is a state-of-the-art single-page application engineered using React, Vite, and Tailwind CSS. Designed to deliver an intuitive, dynamic automation experience, the interface features dynamic schema-driven form generation, live workflow canvas orchestration, WebAuthn passkey biometric security, and Server-Sent Events (SSE) telemetry for real-time execution observation.

## Comprehensive Core UI Capabilities

- **Interactive Workflow Visual Canvas**: A responsive, drag-and-drop directed acyclic graph (DAG) builder that enables users to visually sequence application triggers, logical decision branches, data transformation actions, and time-based schedules. Features custom connection path routing and collapsible step parameter drawers.
- **Natural Language AI Workflow Generator**: An interactive modal interface (`NLWorkflowModal`) integrated with the Crescendo AI/ML microservice. Allows users to describe automation goals in conversational prose and automatically generate completely linked, pre-configured canvas workflows in seconds.
- **Real-Time In-App Notification Inbox & Drawer**: A slide-in inbox drawer (`NotificationDrawer`) and animated topbar bell indicator (`NotificationBell`) powered by Server-Sent Events (`useNotificationStream`). Features unread badges, filter tabs, instant mark-all-read actions, deep links to runs/connections/security, and native background desktop alerts.
- **Visual Email Campaign & Template Studio**: Includes a robust drag-and-drop email block editor (`TemplateBlockEditor`) and campaign broadcast dashboard, empowering users to design responsive transactional email structures, manage audience contact directories, and inspect engagement analytics (open pixel hits, click distributions, and SMTP bounce diagnostics).
- **Notification & Workspace Settings (`/settings/notifications`)**: Dedicated preferences portal to toggle in-app alerts by event category and configure per-workflow noise governance (All Runs, Failures Only, Never).
- **Comprehensive Documentation Portal (`/docs`)**: A built-in educational directory and reference portal that guides normal users and developers alike through application setups, OAuth connection procedures, Bring Your Own Key (BYOK) credential guidelines, and detailed public API SDK integration instructions across multiple programming languages.
- **Developer API Key & Tenant Administration**: Dedicated configuration management interfaces enabling secure API key generation, OAuth redirect callback configuration, workspace member administration, and multi-tenant environment switching.

## Architectural Highlights & Dynamic UI Design

- **Dynamic Catalog Form Hydration**: Eliminates hardcoded form components by parsing JSON schema definitions broadcast by the backend application catalog. Input fields, required parameter validations, tooltips, and dynamic selection dropdowns (e.g., retrieving lists of Slack channels, Spotify playlists, or Asana workspaces) are generated automatically at runtime based on OpenAPI specifications.
- **Real-Time Execution Telemetry (SSE)**: Leverages Server-Sent Events via modern browser `EventSource` interfaces to monitor executing workflow runs. Step status badges transition smoothly in real time across queued, running, completed, or errored states directly on the canvas without requiring manual page refresh or aggressive API polling loops.
- **WebAuthn Passkey & passwordless MFA**: Integrates native browser Web Authentication APIs to support passkey registration and authentication. Enables secure biometric or hardware security key log-in verification alongside standardized JWT session token management and automatic session expiration defenses.
- **Monochrome Design Token System**: Engineered with an expressive, high-contrast monochrome dark mode theme using vanilla CSS variables, Radix UI accessible primitive boundaries, customizable spacing scales, and subtle framer-motion interactive transitions to maximize readability and visual excellence.

## Quality Assurance & Component Testing

The frontend incorporates an offline, highly performant automated testing suite powered by **Vitest**:
- **Zero-Credential Offline Verification**: Executes automated unit and component integration tests completely in memory without requiring active internet connectivity, browser rendering binaries, or live production credentials.
- **Complete Contract & Serializer Suite**: Validates dynamic schema serialization, workflow canvas representation converters, expression variable interpolation logic (`{{steps.step_order.field}}`), and UI component reactivity across 76 individual test specifications.

## Directory & Folder Structure

```text
crescendo-frontend/
├── public/                 # Static graphical assets, favicon files, and client manifest specifications
├── src/
│   ├── components/         # Reusable structural user interface primitives, modals, canvas toolbars, and alert notifications
│   ├── pages/              # Primary routing destinations including Dashboard, Canvas Studio, Email Services, Docs, and Settings
│   │   ├── dashboard/      # Main operational dashboards, workflow overviews, and NLWorkflowModal AI builder integrations
│   │   ├── docs/           # Structured user reference guides, API specifications, and SDK usage instructions
│   │   └── settings/       # Workspace tenant configuration, TemplateBlockEditor email tools, and API key management
│   ├── services/           # REST API communication adapters, SSE stream connection handlers, and WebAuthn authenticators
│   ├── styles/             # Global vanilla CSS stylesheets, monochrome theme variable tokens, and animation definitions
│   ├── App.jsx             # Root React application layout, router boundaries, and global toast state provider registration
│   └── main.jsx            # Application mount initialization, Vite Hot Module Replacement execution, and DOM binding
├── package.json            # Node dependency registries, scripts, and package version specifications
├── vitest.config.js        # Vitest test suite runner configuration and test environment mapping
├── vite.config.js          # Vite compilation build parameters and server development proxies
├── .env.example            # Reference configuration template for required client environment variables
└── README.md               # Frontend developer guide and architectural reference summary
```

## Local Setup & Development Commands

```bash
# 1. Navigate into the frontend workspace repository
cd crescendo-frontend

# 2. Install complete Node project dependency modules
npm ci  # or npm install for initial environment synchronization

# 3. Launch the fast Vite local development server (Default binding on http://localhost:3000)
npm run dev

# 4. Execute the offline Vitest unit and component test suite
npm test -- --run

# 5. Compile and bundle optimized production code distribution files
npm run build
```

For customized network backend proxy endpoints or local environment adjustments, copy `.env.example` to `.env.local` and specify your preferred interface variables.
