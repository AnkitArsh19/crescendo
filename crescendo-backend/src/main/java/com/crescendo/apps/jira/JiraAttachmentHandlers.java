package com.crescendo.apps.jira;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Jira Attachment handlers.
 */
@Component
public class JiraAttachmentHandlers {

    private String getBaseUrl(ActionContext context) {
        String domain = context.getCredential("domain");
        if (domain != null && domain.endsWith("/")) {
            domain = domain.substring(0, domain.length() - 1);
        }
        return domain + "/rest/api/3";
    }

    private String getAuth(ActionContext context) {
        String email = context.getCredential("email");
        String token = context.getCredential("apiToken");
        return "Basic " + Base64.getEncoder().encodeToString((email + ":" + token).getBytes(StandardCharsets.UTF_8));
    }

    @ActionMapping(appKey = "jira", actionKey = "jira:issueAttachment:add")
    public Object addAttachment(ActionContext context) throws Exception {
        String issueKey = context.getString("issueKey");
        String fileName = context.getString("fileName");
        if (fileName == null || fileName.isBlank()) fileName = "attachment.txt";
        String fileContent = context.getString("fileContent");

        byte[] fileBytes;
        if (fileContent != null && !fileContent.isBlank()) {
            try {
                fileBytes = Base64.getDecoder().decode(fileContent.trim());
            } catch (IllegalArgumentException e) {
                fileBytes = fileContent.getBytes(StandardCharsets.UTF_8);
            }
        } else {
            fileBytes = new byte[0];
        }

        String boundary = "----CrescendoJiraBoundary" + System.currentTimeMillis();
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        out.write("Content-Type: application/octet-stream\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        out.write(fileBytes);
        out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl(context) + "/issue/" + issueKey + "/attachments"))
                .header("Authorization", getAuth(context))
                .header("X-Atlassian-Token", "no-check")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            return Map.of("error", "Jira Attachment API error (HTTP " + response.statusCode() + ")", "details", response.body());
        }
        return new com.fasterxml.jackson.databind.ObjectMapper().readValue(response.body(), Object.class);
    }

    @ActionMapping(appKey = "jira", actionKey = "jira:issueAttachment:get")
    public Object getAttachment(ActionContext context) throws Exception {
        String attachmentId = context.getString("attachmentId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/attachment/" + attachmentId)
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "jira", actionKey = "jira:issueAttachment:getAll")
    public Object getAllAttachments(ActionContext context) throws Exception {
        String issueKey = context.getString("issueKey");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/issue/" + issueKey + "?fields=attachment")
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "jira", actionKey = "jira:issueAttachment:remove")
    public Object removeAttachment(ActionContext context) throws Exception {
        String attachmentId = context.getString("attachmentId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/attachment/" + attachmentId)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }
}
