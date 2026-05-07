package com.crescendo.enums;

/**
 * Operators used for step condition matching when filtering incoming trigger events.
 * Used to compare incoming event data against defined conditions.
 */
public enum ConditionOperator {
    EQUALS,
    NOT_EQUALS,
    CONTAINS,
    NOT_CONTAINS,
    GREATER_THAN,
    LESS_THAN,
    STARTS_WITH,
    ENDS_WITH,
    EXISTS,
    NOT_EXISTS
}
