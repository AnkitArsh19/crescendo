package com.crescendo.shared.infrastructure.persistence;

import com.crescendo.shared.domain.valueobject.AppKey;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Converter for AppKey value object.
 * Automatically converts between AppKey and String for database storage.
 */
@Converter(autoApply = true)
public class AppKeyConverter implements AttributeConverter<AppKey, String> {

    @Override
    public String convertToDatabaseColumn(AppKey appKey) {
        return appKey == null ? null : appKey.value();
    }

    @Override
    public AppKey convertToEntityAttribute(String value) {
        return value == null ? null : AppKey.of(value);
    }
}
