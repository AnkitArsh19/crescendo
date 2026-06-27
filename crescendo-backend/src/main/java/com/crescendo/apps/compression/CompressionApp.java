package com.crescendo.apps.compression;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for Compression.
 */
@Component
public class CompressionApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "compression",
                "Compression",
                """
                Compress and decompress files.
                
                This integration provides operations for:
                - **Compress**: Compress files into a zip or gzip archive
                - **Decompress**: Decompress zip or gzip archives
                """,
                "/icons/compress.svg", // Generic icon
                AuthType.NONE,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "compression:compress",
                                "name", "Compress",
                                "description", "Compress files into a zip or gzip archive",
                                "configSchema", List.of(
                                        Map.of("key", "binaryPropertyName", "label", "Input Binary Field(s)", "type", "text", "required", true, "default", "data"),
                                        Map.of("key", "outputFormat", "label", "Output Format", "type", "text", "default", "zip"),
                                        Map.of("key", "fileName", "label", "File Name", "type", "text"),
                                        Map.of("key", "binaryPropertyOutput", "label", "Put Output File in Field", "type", "text", "default", "data")
                                )
                        ),
                        Map.of(
                                "actionKey", "compression:decompress",
                                "name", "Decompress",
                                "description", "Decompress zip or gzip archives",
                                "configSchema", List.of(
                                        Map.of("key", "binaryPropertyName", "label", "Input Binary Field(s)", "type", "text", "required", true, "default", "data"),
                                        Map.of("key", "outputPrefix", "label", "Output Prefix", "type", "text", "default", "file_")
                                )
                        )
                )
        ).credentialSchema(List.of()).category("files-and-storage");
    }
}
