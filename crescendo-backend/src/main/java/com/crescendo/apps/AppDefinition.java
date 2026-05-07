package com.crescendo.apps;

import com.crescendo.app.App;

/**
 * Contract for self-registering app integrations.
 *
 * <p>Every integration module implements this interface to declare its
 * {@link App} catalog entry (name, description, auth type, triggers, actions).
 * The {@link com.crescendo.config.DataSeeder} auto-discovers all
 * {@code AppDefinition} beans and seeds them into the database on startup.
 *
 * <p><strong>To add a new integration:</strong>
 * <ol>
 *   <li>Create a package under {@code com.crescendo.apps.myapp}</li>
 *   <li>Create a class implementing {@code AppDefinition}, annotated with {@code @Component}</li>
 *   <li>Create one or more {@code ActionHandler} classes with {@code @ActionMapping}</li>
 *   <li>That's it — everything is auto-discovered</li>
 * </ol>
 *
 * @see com.crescendo.execution.action.ActionHandler
 * @see com.crescendo.execution.action.ActionMapping
 */
public interface AppDefinition {

    /**
     * Returns the fully populated App entity for this integration.
     * Called once at startup to seed the app catalog.
     */
    App toApp();
}
