package com.crescendo.apps.rss;

import com.crescendo.execution.trigger.TriggerPoller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.*;

@Component
public class RssTriggerPoller implements TriggerPoller {

    private static final Logger logger = LoggerFactory.getLogger(RssTriggerPoller.class);

    @Override
    public boolean supports(String appKey, String triggerKey) {
        return "rss".equals(appKey) && "new-item".equals(triggerKey);
    }

    @Override
    public List<Map<String, Object>> poll(Map<String, Object> credentials, Map<String, Object> configuration, Instant lastPollTime) {
        List<Map<String, Object>> events = new ArrayList<>();
        if (configuration == null) return events;

        String feedUrl = configuration.get("feedUrl") != null ? String.valueOf(configuration.get("feedUrl")) : null;
        if (feedUrl == null || feedUrl.isBlank()) return events;

        try {
            RestClient client;
            if ("true".equalsIgnoreCase(String.valueOf(configuration.get("ignoreSSL")))) {
                javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                        new javax.net.ssl.X509TrustManager() {
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                        }
                };
                javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("TLS");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder().sslContext(sc).build();
                client = RestClient.builder().requestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory(httpClient)).build();
            } else {
                client = RestClient.create();
            }

            String xmlContent = client.get()
                    .uri(feedUrl)
                    .retrieve()
                    .body(String.class);

            String customFieldsStr = configuration.get("customFields") != null ? String.valueOf(configuration.get("customFields")) : "";
            
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(new org.xml.sax.InputSource(new java.io.StringReader(xmlContent)));
            
            org.w3c.dom.NodeList items = doc.getElementsByTagName("item");
            if (items.getLength() == 0) items = doc.getElementsByTagName("entry"); // atom fallback
            
            List<String> customFields = new ArrayList<>();
            if (customFieldsStr != null && !customFieldsStr.isBlank()) {
                for (String f : customFieldsStr.split(",")) customFields.add(f.trim());
            }
            
            for (int i = 0; i < items.getLength(); i++) {
                org.w3c.dom.Element item = (org.w3c.dom.Element) items.item(i);
                Map<String, Object> map = new LinkedHashMap<>();
                
                map.put("title", getTagValue("title", item));
                map.put("link", getTagValue("link", item));
                if ("true".equalsIgnoreCase(String.valueOf(configuration.get("includeFullDescription")))) {
                    map.put("description", getTagValue("description", item));
                }
                map.put("pubDate", getTagValue("pubDate", item));
                map.put("guid", getTagValue("guid", item));
                
                if (map.get("pubDate") == null) map.put("pubDate", getTagValue("updated", item)); // atom fallback
                if (map.get("guid") == null) map.put("guid", getTagValue("id", item)); // atom fallback
                
                for (String cf : customFields) {
                    map.put(cf, getTagValue(cf, item));
                }
                
                events.add(map);
            }
            
        } catch (Exception e) {
            logger.error("[rss] Poll failed: {}", feedUrl, e);
        }

        return events;
    }
    
    private String getTagValue(String tag, org.w3c.dom.Element element) {
        org.w3c.dom.NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList != null && nodeList.getLength() > 0) {
            org.w3c.dom.Node node = nodeList.item(0);
            if (node.getChildNodes().getLength() > 0) {
                return node.getTextContent();
            }
        }
        return null;
    }
}
