# Crescendo AI/ML Module

The AI/ML microservice for Crescendo is a high-performance Python FastAPI service designed to power natural language workflow generation, automated integration catalog scanning, and intelligent AI builder assistance across the automation platform. Built for low-latency asynchronous processing, it integrates directly with high-speed inference engines like Groq to synthesize valid workflow execution graphs from natural language prompts.

## Core Capabilities & Features

- **Natural Language Workflow Builder**: A robust LangGraph-powered state machine that converts conversational user goals into fully formed, validated multi-step Crescendo workflow JSON structures. It supports multi-turn conversation memory, dynamic resource binding, and true DAG conditional branching.
- **Agentic AI Node (`/v1/agent/next-step`)**: An intelligent ReAct loop executor that acts as a runtime oracle for Crescendo workflows. It evaluates context, validates tool schemas, and manages conversation token windows to dynamically call catalog tools and return intelligent final answers.
- **Integration Catalog Enrichment & Sync**: Continually scans and indexes Crescendo's application catalog (`catalog_sync.py`), aligning action schemas, required input attributes, and data definitions with the core Java backend engine.
- **Asynchronous FastAPI Engine**: Powered by Starlette and Pydantic, ensuring non-blocking execution, strong payload schema validation, and concurrent API routing under high request volume.

## Technologies, Frameworks, and Tools

- **FastAPI & Uvicorn**: High-performance asynchronous API framework and ASGI server.
- **LangGraph**: Framework used for building the stateful, multi-agent Natural Language Workflow Builder.
- **Redis**: Provides distributed checkpointer memory for LangGraph, allowing the AI to persist multi-turn conversational context seamlessly.
- **Groq API**: Lightning-fast LLM inference engine. We utilize `llama-3.1-8b-instant` for rapid intent classification and `llama-3.3-70b-versatile` for complex app resolution and ReAct agent tool-calling.
- **Pydantic**: Enforces strict payload validation and JSON contract consistency between Python and Java.

## Technical Architecture & Integration

The AI/ML service functions as the intelligent brain for the primary Java backend:
1. **Request Reception**: Receives natural language generation requests from the frontend canvas modal or runtime tool-calling requests from the Java `AgentExecutionService` orchestrator.
2. **Stateful Graph Execution**: For the workflow builder, it leverages LangGraph and Redis to handle asynchronous, multi-turn clarifications with the user before finalizing a workflow.
3. **Structured Schema Output**: Enforces strict JSON output formatting on LLM responses, ensuring generated workflows conform precisely to Crescendo's Directed Acyclic Graph (DAG) schema and that Agent tool arguments are strictly typed.
4. **Security Enforcement**: Implements bearer token validation and strict CORS policies to protect generation endpoints from unauthorized access.

## Directory & File Structure

```text
crescendo-aiml/
├── app/
│   ├── __init__.py
│   ├── main.py                 # FastAPI application root, CORS configuration, and middleware registration
│   ├── routers/                # REST API endpoints for workflow synthesis, catalog synchronization, and health checks
│   ├── services/               # LLM integration logic, Groq client handlers, and prompt formatting pipelines
│   └── models/                 # Pydantic data models for request validation and structured JSON output
├── catalog_sync.py             # Standalone synchronization utility for indexing backend application handlers
├── requirements.txt            # Python dependencies (FastAPI, Uvicorn, Pydantic, Groq, HTTPX)
├── .env.example                # Template for environment configuration variables
└── README.md                   # Module documentation and usage instructions
```

## Environment Configuration

Before launching the service locally, copy `.env.example` to `.env` and configure the essential operational variables:

```ini
# Server Binding
PORT=8000
HOST=0.0.0.0

# LLM Inference Provider (Groq API)
GROQ_API_KEY=your_groq_api_key_here
MODEL_NAME=llama-3.3-70b-versatile

# Security & CORS Settings
CORS_ORIGINS=http://localhost:3000,http://localhost:8080,https://app.crescendo.run
JWT_SECRET=your_shared_jwt_secret
```

## Local Development & Setup Instructions

```bash
# 1. Navigate to the AI/ML module directory
cd crescendo-aiml

# 2. Create and activate an isolated Python virtual environment
python -m venv venv
source venv/bin/activate        # On Linux/macOS
# venv\Scripts\activate         # On Windows (Command Prompt or PowerShell)

# 3. Install required Python packages and dependency packages
pip install --upgrade pip
pip install -r requirements.txt

# 4. Initialize environment variables from example template
cp .env.example .env

# 5. Start the FastAPI development server with hot-reload enabled
uvicorn app.main:app --reload --port 8000
```

## Testing & Interactive API Documentation

Once the local server is operational on port 8000, FastAPI automatically generates interactive API verification portals:
- **Swagger UI Interactive Exploration**: Visit `http://localhost:8000/docs` to inspect endpoint request models, test natural language generation prompts directly in your browser, and verify JSON response schemas.
- **ReDoc Technical Specification**: Visit `http://localhost:8000/redoc` for comprehensive, read-only OpenAPI architectural documentation.
- **Health Check Confirmation**: Perform a basic curl request to verify engine readiness and connectivity:
```bash
curl -X GET http://localhost:8000/health
```
