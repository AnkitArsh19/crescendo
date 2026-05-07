package com.crescendo.execution.action;

/**
 * Contract for every executable action in the workflow engine.
 * Implementations are discovered automatically via {@link ActionMapping}.
 */
public interface ActionHandler {

    ActionResult execute(ActionContext context);
}
