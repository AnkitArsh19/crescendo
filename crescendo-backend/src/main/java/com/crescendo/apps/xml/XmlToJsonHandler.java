package com.crescendo.apps.xml;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ActionMapping(appKey = "xml", actionKey = "to-json")
public class XmlToJsonHandler implements ActionHandler {
    @Override
public ActionResult execute(ActionContext context) {
        Object xml = context.configuration().get("xml");
        if (xml == null) return ActionResult.failure("XML to JSON requires xml");
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            Element root = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(String.valueOf(xml))))
                    .getDocumentElement();
            return ActionResult.success(Map.of("root", root.getNodeName(), "data", elementToMap(root)));
        } catch (Exception e) {
            return ActionResult.failure("Failed to parse XML: " + e.getMessage());
        }
    }

    private Map<String, Object> elementToMap(Element element) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < element.getAttributes().getLength(); i++) {
            Node attr = element.getAttributes().item(i);
            result.put("@" + attr.getNodeName(), attr.getNodeValue());
        }
        boolean hasElementChildren = false;
        for (int i = 0; i < element.getChildNodes().getLength(); i++) {
            Node child = element.getChildNodes().item(i);
            if (child instanceof Element childElement) {
                hasElementChildren = true;
                result.put(childElement.getNodeName(), elementToMap(childElement));
            }
        }
        String text = element.getTextContent() == null ? "" : element.getTextContent().trim();
        if (!text.isBlank() && !hasElementChildren) {
            result.put("text", text);
        }
        return result;
    }
}
