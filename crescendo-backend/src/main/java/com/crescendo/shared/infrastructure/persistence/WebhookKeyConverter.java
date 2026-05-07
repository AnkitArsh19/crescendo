package com.crescendo.shared.infrastructure.persistence;

import com.crescendo.shared.domain.valueobject.WebhookKey;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Converter for WebhookKey value object.
 * Automatically converts between WebhookKey and String for database storage.
 */
@Converter(autoApply = true)
public class WebhookKeyConverter implements AttributeConverter<WebhookKey, String> {

    @Override
    public String convertToDatabaseColumn(WebhookKey webhookKey) {
        return webhookKey == null ? null : webhookKey.value();
    }

    @Override
    public WebhookKey convertToEntityAttribute(String value) {
        return value == null ? null : WebhookKey.of(value);
    }
}
