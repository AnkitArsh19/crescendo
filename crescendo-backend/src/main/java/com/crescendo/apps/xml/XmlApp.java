package com.crescendo.apps.xml;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for XML.
 */
@Component
public class XmlApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "xml",
                "XML",
                """
                Convert data from and to XML format.
                
                This integration provides operations for:
                - **Convert XML to JSON**: Parses an XML string into a JSON object
                - **Convert JSON to XML**: Serializes a JSON object into an XML string
                """,
                "/icons/xml.svg", // Generic icon
                AuthType.NONE,
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "xml:convert",
                                "name", "Convert XML/JSON",
                                "description", "Convert data between XML and JSON formats",
                                "configSchema", List.of(
                                        Map.of("key", "mode", "label", "Mode", "type", "text", "required", true, "default", "xmlToJson"),
                                        Map.of("key", "dataPropertyName", "label", "Property Name", "type", "text", "required", true, "default", "data"),
                                        Map.of("key", "options", "label", "Options", "type", "json")
                                )
                        )
                )
        ).credentialSchema(List.of()).category("data-transformation");
    }
}
