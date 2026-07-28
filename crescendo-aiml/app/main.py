import asyncio
import logging

from dotenv import load_dotenv

# Load .env before any module that reads env vars (Groq key, service token, Redis URL)
load_dotenv()

from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.catalog_sync import start_catalog_sync
from app.routers.workflow_builder import router
from app.session.checkpointer import init_checkpointer, get_checkpointer
from app.agents.graph import init_graph
from app.templates.matcher import encode_templates_at_startup

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s  %(levelname)-8s  %(name)s — %(message)s",
)

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # ── Startup ─────────────────────────────────────────────────────────────

    # 1. Catalog sync (polls Java backend every 30 s)
    await start_catalog_sync()

    # 2. Redis-backed LangGraph checkpointer (falls back to MemorySaver)
    await init_checkpointer()

    # 3. Compile the LangGraph pipeline (uses the checkpointer)
    init_graph(get_checkpointer())

    # 4. Pre-encode workflow templates via Groq embedding API (~200 ms, one-time)
    #    Done in a thread so it doesn't block if Groq is slow at startup
    try:
        await asyncio.wait_for(encode_templates_at_startup(), timeout=15.0)
    except asyncio.TimeoutError:
        logger.warning("Template encoding timed out — fast-path will be disabled until next restart")
    except Exception as exc:
        logger.warning("Template encoding failed (%s) — fast-path disabled", exc)

    logger.info("Crescendo AI/ML service ready")
    yield

    # ── Shutdown ────────────────────────────────────────────────────────────
    # (nothing to tear down — connections are pooled and managed by libraries)


app = FastAPI(
    title="Crescendo AI/ML Service",
    description=(
        "Internal microservice — proxied via the Java backend. "
        "Converts natural-language prompts into structured workflow specs "
        "using a stateful LangGraph pipeline backed by Groq LLMs."
    ),
    version="3.0.0",
    lifespan=lifespan,
)


@app.get("/", tags=["health"], summary="Health check")
def health_check():
    from app.catalog_sync import app_state
    return {
        "status": "ok",
        "service": "crescendo-aiml",
        "version": "3.0.0",
        "catalog_version": app_state["catalog_version"],
    }


app.include_router(router)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)