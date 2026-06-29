import logging

from dotenv import load_dotenv

# Load .env before any module that reads env vars (Groq key, service token)
load_dotenv()

from fastapi import FastAPI

from app.routers.workflow_builder import router

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s  %(levelname)-8s  %(name)s — %(message)s",
)

app = FastAPI(
    title="Crescendo AI/ML Service",
    description="Internal microservice — proxied via the Java backend. Converts natural-language prompts into structured workflow specs using an LLM.",
    version="0.1.0",
)

# Health check (unauthenticated)
@app.get("/", tags=["health"], summary="Health check")
def health_check():
    return {"status": "ok", "service": "crescendo-aiml"}


# Workflow builder routes — POST /v1/workflow-drafts
app.include_router(router)