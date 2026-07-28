"""
Shared AsyncGroq client
========================
Single module-level instance built lazily on first use, then reused across
all pipeline stages.  All Groq calls in Phase 3 are async — never block the
uvicorn event loop.
"""

import logging
import os
from typing import Optional

from groq import AsyncGroq

logger = logging.getLogger(__name__)

_client: Optional[AsyncGroq] = None


def get_groq_client() -> AsyncGroq:
    """Return the shared AsyncGroq client, creating it on first call."""
    global _client
    if _client is None:
        api_key = os.getenv("GROQ_API_KEY", "")
        if not api_key:
            raise RuntimeError("GROQ_API_KEY environment variable is not set")
        _client = AsyncGroq(api_key=api_key)
        logger.info("AsyncGroq client initialized")
    return _client
