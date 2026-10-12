"""
Semantic Catalog Tool-RAG Retriever
====================================
Uses vector cosine similarity over pre-indexed catalog embeddings to retrieve
the Top-K most relevant application candidates for the Natural Language
Workflow Builder.

Key Architecture Principles:
1. Candidate Narrowing Only: Retrieves Top-K candidates for the reasoning LLM
   to inspect, map parameters, and connect. The LLM still reasons; retrieval
   never blindly executes an action.
2. Multi-Turn Context Aware: Embeds the combined intent and turn context so
   disambiguation ("use Slack") pulls in new tools on follow-up turns.
3. Fallback Discipline: If the embedding API throws an exception or the highest
   cosine similarity is below confidence threshold tau, it gracefully falls back
   to keyword frequency matching without breaking the user request.
4. Canvas Safety: Scoped strictly to design-time builder. Never runs against
   runtime Agent Nodes where canvas tools are explicitly curated.
"""

import asyncio
import json
import logging
import os
import re
import urllib.request
from pathlib import Path
from typing import Any, Dict, List, Optional, Set, Tuple

import numpy as np

logger = logging.getLogger(__name__)

# Default confidence threshold calibrated empirically via benchmark sweep
DEFAULT_CONFIDENCE_THRESHOLD = float(os.getenv("TOOL_RAG_CONFIDENCE_THRESHOLD", "0.35"))
DEFAULT_TOP_K = int(os.getenv("TOOL_RAG_TOP_K", "12"))

# Module-level vector cache
_catalog_keys: Optional[List[str]] = None
_catalog_embeddings: Optional[np.ndarray] = None  # shape: (N, D), normalized

_STOPWORDS = {
    "a", "an", "the", "and", "or", "but", "in", "on", "at", "to", "for",
    "of", "with", "by", "from", "is", "it", "my", "me", "get", "set",
    "new", "all", "any", "do", "when", "then", "that", "this",
    "if", "else", "trigger", "workflow", "create", "send", "use"
}


# ---------------------------------------------------------------------------
# In-Memory Vector Store Initialization
# ---------------------------------------------------------------------------

def init_vector_store(cache_path: Optional[Path] = None) -> bool:
    """
    Load pre-indexed catalog embeddings from .npz cache into memory.
    Safe to call multiple times (idempotent).
    """
    global _catalog_keys, _catalog_embeddings

    if _catalog_embeddings is not None and _catalog_keys is not None:
        return True

    if cache_path is None:
        cache_path = Path(__file__).resolve().parent / "catalog_embeddings_114.npz"

    if not cache_path.exists():
        logger.warning("Tool-RAG vector cache not found at %s. Semantic retrieval will fallback until indexed.", cache_path)
        return False

    try:
        data = np.load(cache_path, allow_pickle=True)
        _catalog_keys = list(data["keys"])
        _catalog_embeddings = data["embeddings"]
        logger.info("Tool-RAG vector store loaded: %d apps, vector dim %d", len(_catalog_keys), _catalog_embeddings.shape[1])
        return True
    except Exception as exc:
        logger.error("Failed to load Tool-RAG vector cache: %s", exc)
        return False


# ---------------------------------------------------------------------------
# Query Embedding Service
# ---------------------------------------------------------------------------

def _embed_query_sync(text: str) -> np.ndarray:
    """Synchronous HTTP call to Gemini embedding endpoint."""
    api_key = os.getenv("GEMINI_API_KEY", "").strip()
    if not api_key:
        raise RuntimeError("GEMINI_API_KEY is not set for Tool-RAG query embedding")

    url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent?key={api_key}"
    body = json.dumps({"content": {"parts": [{"text": text}]}}).encode("utf-8")
    req = urllib.request.Request(url, data=body, headers={"Content-Type": "application/json"})

    with urllib.request.urlopen(req, timeout=10) as resp:
        data = json.loads(resp.read().decode())
        values = data["embedding"]["values"]
        vec = np.array(values, dtype=np.float32)
        norm = np.linalg.norm(vec)
        if norm > 0:
            vec /= norm
        return vec


async def embed_query(text: str) -> np.ndarray:
    """Asynchronously embed query string without blocking the event loop."""
    return await asyncio.to_thread(_embed_query_sync, text)


# ---------------------------------------------------------------------------
# Fallback Keyword Matching
# ---------------------------------------------------------------------------

def _extract_keywords(text: str) -> Set[str]:
    words = re.findall(r"[a-z0-9\-]+", text.lower())
    return {w for w in words if w not in _STOPWORDS and len(w) > 2}


def _op_text(ops: list) -> List[str]:
    parts = []
    for op in ops:
        if isinstance(op, dict):
            parts.append(f"{op.get('key', '')} {op.get('name', '')} {op.get('description', '')}")
        elif isinstance(op, str):
            parts.append(op)
    return parts


def keyword_fallback_filter(
    prompt: str,
    catalog: List[Dict[str, Any]],
    k: int = DEFAULT_TOP_K,
    has_branching: bool = False
) -> List[Dict[str, Any]]:
    """Deterministic fallback: score by keyword overlap."""
    keywords = _extract_keywords(prompt)
    scored: List[Tuple[int, Dict[str, Any]]] = []
    logic_app: Optional[Dict[str, Any]] = None

    for app in catalog:
        app_key = app.get("appKey", "")
        if app_key == "logic":
            logic_app = app

        trig_texts = _op_text(app.get("triggers", []))
        act_texts = _op_text(app.get("actions", []))
        app_text = " ".join([app_key, app.get("name", ""), app.get("description", "")] + trig_texts + act_texts).lower()
        score = sum(1 for kw in keywords if kw in app_text)
        scored.append((score, app))

    scored.sort(key=lambda x: (-x[0], x[1].get("appKey", "")))
    top = [app for _, app in scored[:k]]

    if has_branching and logic_app and logic_app not in top:
        if len(top) >= k:
            top[-1] = logic_app
        else:
            top.append(logic_app)

    return top


# ---------------------------------------------------------------------------
# Main Tool-RAG Retrieval Function
# ---------------------------------------------------------------------------

async def retrieve_candidate_apps(
    prompt: str,
    catalog: List[Dict[str, Any]],
    k: int = DEFAULT_TOP_K,
    has_branching: bool = False,
    threshold: float = DEFAULT_CONFIDENCE_THRESHOLD,
    force_fallback: bool = False
) -> Tuple[List[Dict[str, Any]], float, bool]:
    """
    Retrieves Top-K candidate apps using semantic cosine similarity.
    
    Returns:
        (candidate_apps, max_similarity_score, is_fallback_used)
    """
    global _catalog_keys, _catalog_embeddings

    # Check for deliberate test fallback or uninitialized cache
    if force_fallback or not init_vector_store():
        logger.info("Tool-RAG using fallback keyword filter (store uninitialized or forced)")
        return keyword_fallback_filter(prompt, catalog, k=k, has_branching=has_branching), 0.0, True

    try:
        # Embed the query
        query_vec = await embed_query(prompt)
    except Exception as exc:
        logger.warning("Tool-RAG embedding query failed (%s) — gracefully falling back to keyword filter", exc)
        return keyword_fallback_filter(prompt, catalog, k=k, has_branching=has_branching), 0.0, True

    # Vector dot product (cosine similarity)
    scores = _catalog_embeddings @ query_vec
    max_score = float(np.max(scores))

    # Low-confidence threshold check
    if max_score < threshold:
        logger.debug("Tool-RAG top score %.3f < threshold %.3f — falling back to keyword filter", max_score, threshold)
        return keyword_fallback_filter(prompt, catalog, k=k, has_branching=has_branching), max_score, True

    # Rank apps by score
    ranked_indices = np.argsort(-scores)
    top_keys = [_catalog_keys[i] for i in ranked_indices[:k]]

    # Map keys to full app dicts in catalog
    cat_by_key = {a.get("appKey"): a for a in catalog}
    candidate_apps = [cat_by_key[k] for k in top_keys if k in cat_by_key]

    # Always ensure logic app is present for branching workflows
    if has_branching and "logic" in cat_by_key:
        logic_app = cat_by_key["logic"]
        if logic_app not in candidate_apps:
            if len(candidate_apps) >= k:
                candidate_apps[-1] = logic_app
            else:
                candidate_apps.append(logic_app)

    logger.debug("Tool-RAG retrieved %d candidate apps (max_score=%.3f, threshold=%.3f)", len(candidate_apps), max_score, threshold)
    return candidate_apps, max_score, False
