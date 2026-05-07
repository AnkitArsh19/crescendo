/**
 * Shared Kernel - Domain Driven Design Support
 * 
 * This package contains cross-cutting DDD building blocks used across all bounded contexts:
 * 
 * <h2>Value Objects (com.crescendo.shared.domain.valueobject)</h2>
 * Immutable objects that represent domain concepts with no identity:
 * <ul>
 *   <li>{@link com.crescendo.shared.domain.valueobject.Email} - Validated email address</li>
 *   <li>{@link com.crescendo.shared.domain.valueobject.Username} - Validated username</li>
 *   <li>{@link com.crescendo.shared.domain.valueobject.DomainName} - Validated domain name</li>
 *   <li>{@link com.crescendo.shared.domain.valueobject.AppKey} - Application identifier</li>
 *   <li>{@link com.crescendo.shared.domain.valueobject.ActionKey} - Step action identifier</li>
 *   <li>{@link com.crescendo.shared.domain.valueobject.WebhookKey} - Webhook identifier</li>
 *   <li>{@link com.crescendo.shared.domain.valueobject.StepOrder} - Step ordering (fractional)</li>
 *   <li>{@link com.crescendo.shared.domain.valueobject.GuestSessionId} - Guest user session</li>
 * </ul>
 * 
 * <h2>Building Blocks (com.crescendo.shared.domain)</h2>
 * <ul>
 *   <li>{@link com.crescendo.shared.domain.AggregateRoot} - Base class for aggregate roots with event support</li>
 *   <li>{@link com.crescendo.shared.domain.Entity} - Base class for entities with identity</li>
 * </ul>
 * 
 * <h2>Domain Events (com.crescendo.shared.domain.event)</h2>
 * <ul>
 *   <li>{@link com.crescendo.shared.domain.event.DomainEvent} - Interface for all domain events</li>
 *   <li>{@link com.crescendo.shared.domain.event.BaseDomainEvent} - Base class with common fields</li>
 *   <li>{@link com.crescendo.shared.domain.event.DomainEventPublisher} - Event publishing interface</li>
 * </ul>
 * 
 * <h2>Infrastructure (com.crescendo.shared.infrastructure)</h2>
 * <ul>
 *   <li>JPA Converters for value objects (persistence)</li>
 *   <li>Spring-based domain event publisher (event)</li>
 * </ul>
 * 
 * <h2>Bounded Contexts</h2>
 * The application is organized into bounded contexts:
 * <ul>
 *   <li><b>User</b> - User management, authentication, credentials</li>
 *   <li><b>Workflow</b> - Workflow definitions and configuration</li>
 *   <li><b>Steps</b> - Step definitions within workflows</li>
 *   <li><b>Connections</b> - OAuth connections to external apps</li>
 *   <li><b>Logbook</b> - Execution history (WorkflowRun, StepRun)</li>
 *   <li><b>EmailService</b> - Custom domain email sending</li>
 *   <li><b>Webhook</b> - Incoming webhook triggers</li>
 * </ul>
 * 
 * @see com.crescendo.shared.domain.AggregateRoot
 * @see com.crescendo.shared.domain.valueobject.Email
 */
package com.crescendo.shared;
