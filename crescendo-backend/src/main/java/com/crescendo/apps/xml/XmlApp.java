package com.crescendo.apps.xml;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class XmlApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("xml", "XML", "Convert XML to JSON-like data and JSON-like data to XML",
                "/icons/xml.svg", AuthType.NONE,
                List.of(),
                List.of(
                        Map.of("actionKey", "to-json", "name", "XML to JSON",
                                "description", "Parse an XML string",
                                "configSchema", List.of(
                                        Map.of("key", "xml", "label", "XML", "type", "textarea", "required", true,
                                                "placeholder", "<root><id>1</id></root>", "helpText", "XML string to parse")
                                )),
                        Map.of("actionKey", "to-xml", "name", "JSON to XML",
                                "description", "Create XML from an object",
                                "configSchema", List.of(
                                        Map.of("key", "rootName", "label", "Root Name", "type", "text", "required", false,
                                                "placeholder", "root", "helpText", "Root element name"),
                                        Map.of("key", "data", "label", "Data", "type", "json", "required", true,
                                                "placeholder", "{\"id\":1}", "helpText", "Object to convert")
                                ))
                )
        ).credentialSchema(List.of()).category("core").helpUrl("");
    }
}
