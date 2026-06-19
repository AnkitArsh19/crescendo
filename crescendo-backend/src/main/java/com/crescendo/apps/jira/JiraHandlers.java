package com.crescendo.apps.jira;

import com.crescendo.apps.simpleapi.SimpleApiSupport;
import com.crescendo.execution.action.*;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import java.util.*;

class JiraBase {
    static org.springframework.web.client.RestClient c(ActionContext x) {
        return SimpleApiSupport.basic(
                SimpleApiSupport.cred(x, "baseUrl"),
                SimpleApiSupport.cred(x, "email"),
                SimpleApiSupport.cred(x, "apiToken")
        );
    }
}

@ActionMapping(appKey = "jira", actionKey = "create-issue")
class JiraCreateIssueHandler implements ActionHandler {
    private final ObjectMapper m;

    JiraCreateIssueHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("project", Map.of("key", SimpleApiSupport.cfg(c, "projectKey")));
            fields.put("summary", SimpleApiSupport.cfg(c, "summary"));
            fields.put("issuetype", Map.of("name", SimpleApiSupport.cfg(c, "issueType").isBlank() ? "Task" : SimpleApiSupport.cfg(c, "issueType")));
            String d = SimpleApiSupport.cfg(c, "description");
            if (!d.isBlank()) {
                fields.put("description", Map.of(
                        "type", "doc",
                        "version", 1,
                        "content", List.of(Map.of(
                                "type", "paragraph",
                                "content", List.of(Map.of(
                                        "type", "text",
                                        "text", d
                                ))
                        ))
                ));
            }
            String res = JiraBase.c(c).post()
                    .uri("/rest/api/3/issue")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("fields", fields))
                    .retrieve()
                    .body(String.class);
            return SimpleApiSupport.parsed(m, res);
        } catch (Exception e) {
            return ActionResult.failure("Jira create issue failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "jira", actionKey = "get-issue")
class JiraGetIssueHandler implements ActionHandler {
    private final ObjectMapper m;

    JiraGetIssueHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String res = JiraBase.c(c).get()
                    .uri("/rest/api/3/issue/{key}", SimpleApiSupport.cfg(c, "issueKey"))
                    .retrieve()
                    .body(String.class);
            return SimpleApiSupport.parsed(m, res);
        } catch (Exception e) {
            return ActionResult.failure("Jira get issue failed: " + e.getMessage());
        }
    }
}
