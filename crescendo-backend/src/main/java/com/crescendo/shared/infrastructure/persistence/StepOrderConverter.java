package com.crescendo.shared.infrastructure.persistence;

import com.crescendo.shared.domain.valueobject.StepOrder;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;

/**
 * JPA Converter for StepOrder value object.
 * Automatically converts between StepOrder and BigDecimal for database storage.
 */
@Converter(autoApply = true)
public class StepOrderConverter implements AttributeConverter<StepOrder, BigDecimal> {

    @Override
    public BigDecimal convertToDatabaseColumn(StepOrder stepOrder) {
        return stepOrder == null ? null : stepOrder.value();
    }

    @Override
    public StepOrder convertToEntityAttribute(BigDecimal value) {
        return value == null ? null : StepOrder.of(value);
    }
}
