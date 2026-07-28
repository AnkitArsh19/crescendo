"""
LangGraph StateGraph — Phase 3 Pipeline
========================================
Graph structure:

  [template_node]
       │ hit → [configurator_node] ─────────────────────────────────┐
       │ miss                                                        │
       ↓                                                             │
  [intent_node]                                                      │
       │ clarification needed → [clarify_node] → END                │
       │ clear                                                       │
       ↓                                                             │
  [resolver_node]                                                    │
       │ error → END                                                 │
       ↓                                                             │
  [configurator_node] ←────────────────────────────────────────────┘
       ↓
  [validator_node]
       │ valid → [explainer_node] → END
       │ errors (first try) → [correction_node]
       │ errors (post-correction) → END (error response)
       ↓
  [correction_node] → [validator_node]  (second pass)

Compiled graph is built once at startup and stored as a module-level singleton.
"""

import logging
from typing import Any, Optional

from langgraph.graph import END, StateGraph

from app.agents.nodes import (
    clarify_node,
    configurator_node,
    correction_node,
    explainer_node,
    intent_node,
    resolver_node,
    route_after_intent,
    route_after_resolver,
    route_after_validator,
    route_template,
    template_node,
    validator_node,
)
from app.agents.state import PipelineState

logger = logging.getLogger(__name__)

_graph: Optional[Any] = None   # CompiledGraph — typed as Any to avoid langgraph version coupling


def build_graph(checkpointer: Any) -> Any:
    """Construct and compile the StateGraph with the given checkpointer."""
    g = StateGraph(PipelineState)

    # Register all nodes
    g.add_node("template",      template_node)
    g.add_node("intent",        intent_node)
    g.add_node("clarify",       clarify_node)
    g.add_node("resolver",      resolver_node)
    g.add_node("configurator",  configurator_node)
    g.add_node("validator",     validator_node)
    g.add_node("correction",    correction_node)
    g.add_node("explainer",     explainer_node)

    # Entry point
    g.set_entry_point("template")

    # Conditional edge from template
    g.add_conditional_edges(
        "template",
        route_template,
        {"intent": "intent", "configurator": "configurator", "end": END},
    )

    # Intent → clarify or resolver
    g.add_conditional_edges(
        "intent",
        route_after_intent,
        {"clarify": "clarify", "resolver": "resolver", "end": END},
    )

    # Clarify always terminates (returns questions, saves state)
    g.add_edge("clarify", END)

    # Resolver → configurator (or END on error)
    g.add_conditional_edges(
        "resolver",
        route_after_resolver,
        {"configurator": "configurator", "end": END},
    )

    # Configurator → validator (always — configurator degrades gracefully)
    g.add_edge("configurator", "validator")

    # Validator → explainer, correction, or END
    g.add_conditional_edges(
        "validator",
        route_after_validator,
        {"explainer": "explainer", "correction": "correction", "end": END},
    )

    # Correction → second validation pass
    g.add_edge("correction", "validator")

    # Explainer always terminates
    g.add_edge("explainer", END)

    compiled = g.compile(checkpointer=checkpointer)
    logger.info("LangGraph pipeline compiled successfully")
    return compiled


def init_graph(checkpointer: Any) -> None:
    """Build the graph and store it as the module-level singleton."""
    global _graph
    _graph = build_graph(checkpointer)
    logger.info("Graph singleton initialised")


def get_graph() -> Any:
    """Return the compiled graph; raises if not yet initialised."""
    if _graph is None:
        raise RuntimeError("Graph not initialised — call init_graph() first in lifespan")
    return _graph
