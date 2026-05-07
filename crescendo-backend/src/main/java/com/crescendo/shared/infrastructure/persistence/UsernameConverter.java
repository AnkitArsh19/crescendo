package com.crescendo.shared.infrastructure.persistence;

import com.crescendo.shared.domain.valueobject.Username;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Converter for Username value object.
 * Automatically converts between Username and String for database storage.
 */
@Converter(autoApply = true)
public class UsernameConverter implements AttributeConverter<Username, String> {

    @Override
    public String convertToDatabaseColumn(Username username) {
        return username == null ? null : username.value();
    }

    @Override
    public Username convertToEntityAttribute(String value) {
        return value == null ? null : Username.of(value);
    }
}
