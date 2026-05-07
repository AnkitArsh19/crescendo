package com.crescendo.shared.infrastructure.persistence;

import com.crescendo.shared.domain.valueobject.DomainName;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Converter for DomainName value object.
 * Automatically converts between DomainName and String for database storage.
 */
@Converter(autoApply = true)
public class DomainNameConverter implements AttributeConverter<DomainName, String> {

    @Override
    public String convertToDatabaseColumn(DomainName domainName) {
        return domainName == null ? null : domainName.value();
    }

    @Override
    public DomainName convertToEntityAttribute(String value) {
        return value == null ? null : DomainName.of(value);
    }
}
