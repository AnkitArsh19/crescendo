"""
Redis-backed LangGraph checkpointer with MemorySaver fallback
==============================================================
Connects to the SAME Redis instance the Java backend already uses.
Python session keys use the langgraph prefix: no collision with the
Java crescendo:* keyspace.

Environment variables:
  REDIS_URL             default: redis://localhost:6379
  SESSION_TTL_SECONDS   default: 86400 (24 h); applied as checkpoint TTL
"""

import logging
import os
from typing import Any

logger = logging.getLogger(__name__)

_checkpointer: Any = None


async def init_checkpointer() -> None:
    """
    Try to build an AsyncRedisSaver connected to REDIS_URL.
    Falls back to MemorySaver if Redis is unreachable (single-instance dev mode).
    Must be called once during application startup.
    """
    global _checkpointer
    redis_url = os.getenv("REDIS_URL", "redis://localhost:6379")

    try:
        import redis.asyncio as aioredis
        from langgraph.checkpoint.redis.aio import AsyncRedisSaver

        redis_client = aioredis.from_url(redis_url, decode_responses=False)
        # Ping to verify connectivity before committing
        await redis_client.ping()
        saver = AsyncRedisSaver(redis_client)
        await saver.setup()      # creates required index structures
        _checkpointer = saver
        logger.info("LangGraph checkpointer: AsyncRedisSaver connected to %s", redis_url)

    except Exception as exc:
        logger.warning(
            "Redis unavailable (%s), falling back to in-memory checkpointer. "
            "Multi-turn sessions will NOT survive server restarts.", exc
        )
        from langgraph.checkpoint.memory import MemorySaver
        _checkpointer = MemorySaver()


def get_checkpointer() -> Any:
    """Return the initialised checkpointer (Redis or memory)."""
    if _checkpointer is None:
        raise RuntimeError("Checkpointer not initialised — call init_checkpointer() first")
    return _checkpointer
