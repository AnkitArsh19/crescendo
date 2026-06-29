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

from contextlib import asynccontextmanager
from app.catalog_sync import start_catalog_sync

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    await start_catalog_sync()
    yield
    # Shutdown (if needed)

app = FastAPI(
    title="Crescendo AI/ML Service",
    description="Internal microservice — proxied via the Java backend. Converts natural-language prompts into structured workflow specs using an LLM.",
    version="0.1.0",
    lifespan=lifespan
)

# Health check (unauthenticated)
@app.get("/", tags=["health"], summary="Health check")
def health_check():
    from app.catalog_sync import app_state
    return {
        "status": "ok", 
        "service": "crescendo-aiml",
        "catalog_version": app_state["catalog_version"]
    }


# Workflow builder routes — POST /v1/workflow-drafts
app.include_router(router)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)