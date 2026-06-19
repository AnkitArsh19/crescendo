package com.crescendo.apps.http;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class HttpApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("http", "HTTP / API", "Make HTTP requests to any REST API endpoint",
                "/icons/http.svg", AuthType.NONE,
                List.of(
                    Map.of("triggerKey", "catch-hook", "name", "Catch Webhook",
                        "description", "Receives inbound webhook payloads",
                        "configSchema", List.of(
                            Map.of("key", "childKey", "label", "Child Key", "type", "text", "required", false,
                                   "placeholder", "data.user", "helpText", "Parse specific JSON key path")))
                ),
                List.of(
                    Map.of("actionKey", "request", "name", "HTTP Request",
                        "description", "Send a GET, POST, PUT, PATCH, or DELETE request",
                        "configSchema", List.of(
                            Map.of("key", "url", "label", "URL", "type", "text", "required", true,
                                   "placeholder", "https://api.example.com/data", "helpText", "Target URL"),
                            Map.of("key", "method", "label", "Method", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "GET", "label", "GET"),
                                       Map.of("value", "POST", "label", "POST"),
                                       Map.of("value", "PUT", "label", "PUT"),
                                       Map.of("value", "PATCH", "label", "PATCH"),
                                       Map.of("value", "DELETE", "label", "DELETE"),
                                       Map.of("value", "HEAD", "label", "HEAD"),
                                       Map.of("value", "OPTIONS", "label", "OPTIONS")
                                   ), "helpText", "HTTP method (default: GET)"),
                            Map.of("key", "queryParams", "label", "Query Params (JSON)", "type", "json", "required", false,
                                   "placeholder", "{\"page\": 1}", "helpText", "Query string parameters"),
                            Map.of("key", "headers", "label", "Headers (JSON)", "type", "json", "required", false,
                                   "placeholder", "{\"Authorization\": \"Bearer xxx\"}", "helpText", "Request headers"),
                            Map.of("key", "bodyType", "label", "Body Type", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "none", "label", "None"),
                                       Map.of("value", "json", "label", "JSON"),
                                       Map.of("value", "raw", "label", "Raw"),
                                       Map.of("value", "form", "label", "Form URL Encoded"),
                                       Map.of("value", "multipart", "label", "Multipart Form")
                                   ), "helpText", "Body encoding"),
                            Map.of("key", "body", "label", "JSON Body", "type", "json", "required", false,
                                   "placeholder", "{\"key\": \"value\"}", "helpText", "JSON body for POST/PUT/PATCH"),
                            Map.of("key", "rawBody", "label", "Raw Body", "type", "textarea", "required", false,
                                   "placeholder", "plain text", "helpText", "Raw request body"),
                            Map.of("key", "formData", "label", "Form Data (JSON)", "type", "json", "required", false,
                                   "placeholder", "{\"name\":\"value\"}", "helpText", "Form fields for URL-encoded or multipart requests"),
                            Map.of("key", "file", "label", "File", "type", "file", "required", false,
                                   "helpText", "Optional multipart file upload"),
                            Map.of("key", "fileFieldName", "label", "File Field Name", "type", "text", "required", false,
                                   "placeholder", "file", "helpText", "Multipart field name for the file"),
                            Map.of("key", "basicAuth", "label", "Basic Auth", "type", "text", "required", false,
                                   "placeholder", "username:password", "helpText", "Optional basic auth"),
                            Map.of("key", "responseType", "label", "Response Type", "type", "select", "required", false,
                                   "options", List.of(Map.of("value", "text", "label", "Text / JSON"), Map.of("value", "binary", "label", "Binary Base64")),
                                   "helpText", "How to return the response body"),
                            Map.of("key", "timeoutSeconds", "label", "Timeout Seconds", "type", "number", "required", false,
                                   "placeholder", "30", "helpText", "Request timeout"),
                            Map.of("key", "followRedirects", "label", "Follow Redirects", "type", "boolean", "required", false,
                                   "helpText", "Follow HTTP redirects"),
                            Map.of("key", "allowInsecureSsl", "label", "Allow Self-Signed SSL", "type", "boolean", "required", false,
                                   "helpText", "Only use for trusted internal endpoints"),
                            Map.of("key", "paginationMode", "label", "Pagination", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "none", "label", "None"),
                                       Map.of("value", "page", "label", "Page Number"),
                                       Map.of("value", "offset", "label", "Offset")
                                   ), "helpText", "Simple no-code pagination"),
                            Map.of("key", "maxPages", "label", "Max Pages", "type", "number", "required", false,
                                   "placeholder", "1", "helpText", "Maximum pages/requests"),
                            Map.of("key", "pageParam", "label", "Page Param", "type", "text", "required", false,
                                   "placeholder", "page", "helpText", "Query param used for page number"),
                            Map.of("key", "pageStart", "label", "Start Page", "type", "number", "required", false,
                                   "placeholder", "1", "helpText", "First page number"),
                            Map.of("key", "offsetParam", "label", "Offset Param", "type", "text", "required", false,
                                   "placeholder", "offset", "helpText", "Query param used for offset"),
                            Map.of("key", "offsetStart", "label", "Start Offset", "type", "number", "required", false,
                                   "placeholder", "0", "helpText", "First offset"),
                            Map.of("key", "limitParam", "label", "Limit Param", "type", "text", "required", false,
                                   "placeholder", "limit", "helpText", "Optional page size query param"),
                            Map.of("key", "limit", "label", "Limit", "type", "number", "required", false,
                                   "placeholder", "100", "helpText", "Page size")
                        ))
                )
        ).credentialSchema(List.of()).category("developer").helpUrl("");
    }
}
