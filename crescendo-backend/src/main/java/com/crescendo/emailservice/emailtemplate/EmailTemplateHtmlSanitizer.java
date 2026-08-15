package com.crescendo.emailservice.emailtemplate;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/**
 * Allows email-oriented markup while removing executable browser content before
 * it reaches the editor preview or an inbox. This is deliberately narrower than
 * a general web-page sanitizer: forms, frames, scripts and event handlers have
 * no useful place in a transactional email.
 */
@Component
public class EmailTemplateHtmlSanitizer {

    public String sanitize(String html) {
        if (html == null || html.isBlank()) return html;
        Document document = Jsoup.parse(html);
        document.select("script,iframe,frame,frameset,object,embed,form,base").remove();
        for (Element element : document.getAllElements()) {
            for (Attribute attribute : element.attributes().asList()) {
                String key = attribute.getKey().toLowerCase();
                String value = attribute.getValue().trim().toLowerCase();
                if (key.startsWith("on") || key.equals("srcdoc")
                        || ((key.equals("href") || key.equals("src")) && value.startsWith("javascript:"))) {
                    element.removeAttr(attribute.getKey());
                }
            }
        }
        return document.outerHtml();
    }
}
