package com.crescendo.apps.compression;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class CompressionApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("compression", "Compression", "Compress and decompress text payloads",
                "/icons/compression.svg", AuthType.NONE,
                List.of(),
                List.of(
                        Map.of("actionKey", "compress", "name", "Compress Text",
                                "description", "Compress text as gzip or zip and return Base64",
                                "configSchema", List.of(
                                        Map.of("key", "format", "label", "Format", "type", "select", "required", false,
                                                "options", List.of(
                                                        Map.of("value", "gzip", "label", "Gzip"),
                                                        Map.of("value", "zip", "label", "Zip")
                                                )),
                                        Map.of("key", "text", "label", "Text", "type", "textarea", "required", true,
                                                "placeholder", "Text to compress"),
                                        Map.of("key", "fileName", "label", "Zip File Name", "type", "text", "required", false,
                                                "placeholder", "data.txt"))),
                        Map.of("actionKey", "decompress", "name", "Decompress Text",
                                "description", "Decompress Base64 gzip or zip data",
                                "configSchema", List.of(
                                        Map.of("key", "format", "label", "Format", "type", "select", "required", false,
                                                "options", List.of(
                                                        Map.of("value", "gzip", "label", "Gzip"),
                                                        Map.of("value", "zip", "label", "Zip")
                                                )),
                                        Map.of("key", "base64", "label", "Base64 Data", "type", "textarea", "required", true)))
                )
        ).credentialSchema(List.of()).category("core").helpUrl("");
    }
}
