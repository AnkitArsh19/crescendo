package com.crescendo.shared.infrastructure.persistence;

import com.crescendo.shared.domain.valueobject.ActionKey;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Converter for ActionKey value object.
 * Automatically converts between ActionKey and String for database storage.
 */
@Converter(autoApply = true)
public class ActionKeyConverter implements AttributeConverter<ActionKey, String> {

    @Override
    public String convertToDatabaseColumn(ActionKey actionKey) {
        return actionKey == null ? null : actionKey.value();
    }

    @Override
    public ActionKey convertToEntityAttribute(String value) {
        return value == null ? null : ActionKey.of(value);
    }
}
