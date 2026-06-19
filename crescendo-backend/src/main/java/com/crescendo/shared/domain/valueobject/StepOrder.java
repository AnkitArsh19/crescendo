package com.crescendo.shared.domain.valueobject;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Value Object representing the order of a step in a workflow.
 * Uses BigDecimal for fractional ordering (allows inserting between steps).
 * Immutable and self-validating.
 */
@Embeddable
public record StepOrder(BigDecimal value) implements Comparable<StepOrder> {

    private static final int SCALE = 6;

    public StepOrder {
        if (value == null) {
            throw new IllegalArgumentException("Step order cannot be null");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Step order cannot be negative");
        }
        // Normalize to consistent scale
        value = value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Factory method for creating StepOrder from BigDecimal.
     */
    public static StepOrder of(BigDecimal value) {
        return new StepOrder(value);
    }

    /**
     * Factory method for creating StepOrder from integer.
     */
    public static StepOrder of(int value) {
        return new StepOrder(BigDecimal.valueOf(value));
    }

    /**
     * Calculate the midpoint between this order and another.
     * Useful for inserting a step between two existing steps.
     */
    public StepOrder midpoint(StepOrder other) {
        BigDecimal sum = this.value.add(other.value);
        BigDecimal mid = sum.divide(BigDecimal.valueOf(2), SCALE, RoundingMode.HALF_UP);
        return new StepOrder(mid);
    }

    /**
     * Get the next whole number order after this one.
     */
    public StepOrder nextWhole() {
        BigDecimal next = value.setScale(0, RoundingMode.CEILING).add(BigDecimal.ONE);
        return new StepOrder(next);
    }

    @Override
    public int compareTo(StepOrder other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
