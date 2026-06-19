package com.crescendo.apps.readpdf;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class ReadPdfApp implements AppDefinition {
    public App toApp() {
        return new App(
                "read-pdf",
                "Read PDF",
                "Extract text from PDF Base64 data",
                "/icons/pdf.svg",
                AuthType.NONE,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "extract-text",
                                "name", "Extract Text",
                                "description", "Read PDF and extract text",
                                "configSchema", List.of(
                                        Map.of("key", "base64", "label", "PDF Base64", "type", "textarea", "required", true),
                                        Map.of("key", "password", "label", "Password", "type", "password", "required", false)
                                )
                        )
                )
        ).credentialSchema(List.of()).category("core").helpUrl("");
    }
}
