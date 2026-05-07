package com.crescendo.shared.infrastructure.persistence;

import com.crescendo.shared.domain.valueobject.GuestSessionId;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Converter for GuestSessionId value object.
 * Automatically converts between GuestSessionId and String for database storage.
 */
@Converter(autoApply = true)
public class GuestSessionIdConverter implements AttributeConverter<GuestSessionId, String> {

    @Override
    public String convertToDatabaseColumn(GuestSessionId guestSessionId) {
        return guestSessionId == null ? null : guestSessionId.value();
    }

    @Override
    public GuestSessionId convertToEntityAttribute(String value) {
        return value == null ? null : GuestSessionId.of(value);
    }
}
