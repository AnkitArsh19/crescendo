package com.crescendo.execution.action;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the {@code appKey:actionKey} pair that an {@link ActionHandler} handles.
 * The annotated class is also registered as a Spring component.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface ActionMapping {

    String appKey();

    String actionKey();
}
