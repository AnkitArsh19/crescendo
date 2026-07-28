"""
Template Embedding Matcher
===========================
Uses the Groq embedding API (nomic-embed-text-v1.5) to encode templates
at startup and match user prompts at request time.

No PyTorch / sentence-transformers — one Groq API call per request, plus
one bulk call at startup for the template library.  All template vectors
are stored in-memory as a numpy array for fast dot-product similarity.

Similarity threshold is tunable via TEMPLATE_SIMILARITY_THRESHOLD env var
(default: 0.92).  Returning None means "no match" — fall through to full pipeline.
"""

import logging
import os
from typing import Optional

import numpy as np
from groq import AsyncGroq

from app.agents.client import get_groq_client
from app.schemas.workflow import WorkflowSpec
from app.templates.workflow_templates import TEMPLATES, WorkflowTemplate

logger = logging.getLogger(__name__)

_EMBED_MODEL = "nomic-embed-text-v1.5"

# Module-level store — populated at startup by encode_templates_at_startup()
_template_embeddings: Optional[np.ndarray] = None   # shape: (N, D)


async def encode_templates_at_startup(groq_client: Optional[AsyncGroq] = None) -> None:
    """
    Encode all template embedding_text strings using Groq's embedding API.
    Call once during application startup.  Safe to call again (idempotent).
    """
    global _template_embeddings
    client = groq_client or get_groq_client()
    texts = [t.embedding_text for t in TEMPLATES]

    try:
        response = await client.embeddings.create(model=_EMBED_MODEL, input=texts)
        vecs = np.array([e.embedding for e in response.data], dtype=np.float32)
        # L2-normalise so dot product == cosine similarity
        norms = np.linalg.norm(vecs, axis=1, keepdims=True)
        norms = np.where(norms == 0, 1.0, norms)
        _template_embeddings = vecs / norms
        logger.info("Template embeddings ready: %d templates encoded", len(TEMPLATES))
    except Exception as exc:
        logger.warning("Template encoding failed (%s) — fast-path disabled", exc)
        _template_embeddings = None


async def match_template(
    prompt: str,
    groq_client: Optional[AsyncGroq] = None,
) -> Optional[WorkflowTemplate]:
    """
    Return the best-matching WorkflowTemplate if cosine similarity ≥ threshold,
    otherwise return None.
    """
    if _template_embeddings is None:
        return None   # not yet initialised

    threshold = float(os.getenv("TEMPLATE_SIMILARITY_THRESHOLD", "0.92"))
    client = groq_client or get_groq_client()

    try:
        response = await client.embeddings.create(model=_EMBED_MODEL, input=[prompt])
        query_vec = np.array(response.data[0].embedding, dtype=np.float32)
        norm = np.linalg.norm(query_vec)
        if norm > 0:
            query_vec /= norm
    except Exception as exc:
        logger.warning("Template matching embedding call failed: %s", exc)
        return None

    scores = _template_embeddings @ query_vec   # cosine similarities, shape (N,)
    best_idx = int(np.argmax(scores))
    best_score = float(scores[best_idx])

    if best_score >= threshold:
        matched = TEMPLATES[best_idx]
        logger.info("Template fast-path hit: id=%s score=%.3f", matched.id, best_score)
        return matched

    logger.debug("No template match (best=%.3f < %.2f) — full pipeline", best_score, threshold)
    return None
