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
    """Return the shared AI client (Gemini or Groq), creating it on first call."""
    global _client
    if _client is None:
        provider = os.getenv("AI_PROVIDER", "gemini").lower()
        gemini_key = os.getenv("GEMINI_API_KEY", "").strip()
        if provider == "gemini" and gemini_key:
            _client = AsyncGroq(api_key=gemini_key, base_url="https://generativelanguage.googleapis.com/v1beta")
            logger.info("AI Client initialized with Google Gemini endpoint")
        else:
            api_key = os.getenv("GROQ_API_KEY", "").strip()
            if not api_key:
                if gemini_key:
                    _client = AsyncGroq(api_key=gemini_key, base_url="https://generativelanguage.googleapis.com/v1beta")
                    logger.info("AI Client fallback to Google Gemini endpoint")
                    return _client
                raise RuntimeError("Neither GEMINI_API_KEY nor GROQ_API_KEY environment variable is set")
            _client = AsyncGroq(api_key=api_key)
            logger.info("AsyncGroq client initialized")
    return _client


# Alias for modern provider-agnostic callers
get_ai_client = get_groq_client

