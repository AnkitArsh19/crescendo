# Contributing to Crescendo

We welcome contributions from developers, designers, and systems architects to help evolve the Crescendo enterprise automation platform. This guide outlines the development standards, PR workflow, and architectural rules required when proposing changes to the repository.

## Development Philosophy & Standards

Crescendo relies on strict modularity, high concurrency, and zero-credential local testability. Regardless of which module you contribute to, follow these universal engineering rules:

1. **Strict Tone & Typography Rules**: Documentation files, inline comments, user-facing labels, and navigation items must contain zero emojis and zero em dashes or hyphen-pairs used as em dashes. Maintain a clean, professional technical writing style.
2. **Zero-Credential Verification**: Never commit secrets, API keys, or personal credentials. All third-party integration connectors and custom tools must be verified using offline mock contracts and simulated HTTP endpoints rather than live network production accounts.
3. **Modular Boundary Compliance**: Respect Command Query Responsibility Segregation (CQRS) boundaries in the Java backend and separation of concerns in the frontend UI design systems.

## Repository Module Overview

- `crescendo-backend/`: Java 25 / Spring Boot execution engine, DAG orchestrator, 114 native application integrations, and transactional email service.
- `crescendo-frontend/`: React 19 / Vite UI application, workflow drawing canvas, template studios, and live SSE tracking dashboards.
- `crescendo-aiml/`: Python 3.12 / FastAPI conversational synthesis service and automated workflow template generators.

## Local Development Setup

### 1. Backend Service (Java / Spring Boot)
Ensure you have JDK 25 and Docker running locally for containerized PostgreSQL and Redis dependencies.
```bash
cd crescendo-backend
./mvnw test
./mvnw spring-boot:run
```

### 2. Frontend Studio (React / Vite)
Ensure Node.js 20+ is installed.
```bash
cd crescendo-frontend
npm ci
npm test -- --run
npm run dev
```

### 3. AI & ML Synthesis Microservice (Python / FastAPI)
Ensure Python 3.12+ is available.
```bash
cd crescendo-aiml
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8001
```

## Pull Request Lifecycle & Review Rules

To submit code or documentation enhancements:

1. **Fork & Branch**: Fork the main repository and create a feature branch off `main` titled `feature/description` or `fix/description`.
2. **Commit Formatting**: Write descriptive commit messages summarizing the technical intent of the change.
3. **Automated CI Validation**: Ensure all automated GitHub Actions checks pass. This includes Java contract tests, Vitest UI suites, and Python code validations.
4. **Pull Request Template**: Complete all sections of the automated PR template upon submission, specifying tested endpoints and module impacts.
5. **Review SLA**: Maintainers review proposals periodically. Feedback addressing architectural cohesion or style rules must be resolved prior to merge approval.

## Found a Bug or Have a Feature Suggestion?
Check existing GitHub issues before submitting new tickets. Use the provided structured issue forms to document reproduction steps, expected behaviors, and log traces.
