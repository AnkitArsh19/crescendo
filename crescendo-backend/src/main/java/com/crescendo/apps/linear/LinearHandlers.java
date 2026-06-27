package com.crescendo.apps.linear;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Linear Issue and Comment handlers.
 */
@Component
public class LinearHandlers {

    private String getBaseUrl() {
        return "https://api.linear.app/graphql";
    }

    private String getAuth(ActionContext context) {
        return context.getCredential("apiToken");
    }

    private Object executeGraphQL(ActionContext context, String query, Map<String, Object> variables) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("query", query);
        body.put("variables", variables);

        return RestClient.builder()
                .url(getBaseUrl())
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    // ─── ISSUE ───

    @ActionMapping(appKey = "linear", actionKey = "linear:issue:create")
    public Object createIssue(ActionContext context) throws Exception {
        String teamId = context.getString("teamId");
        String title = context.getString("title");
        String description = context.getString("description");
        String assigneeId = context.getString("assigneeId");
        String stateId = context.getString("stateId");

        String query = """
            mutation IssueCreate (
                $title: String!,
                $teamId: String!,
                $description: String,
                $assigneeId: String,
                $stateId: String){
                issueCreate(
                    input: {
                        title: $title
                        description: $description
                        teamId: $teamId
                        assigneeId: $assigneeId
                        stateId: $stateId
                    }
                ) {
                    success
                    issue {
                        id
                        identifier
                        title
                        description
                    }
                }
            }
        """;

        Map<String, Object> variables = new HashMap<>();
        variables.put("title", title);
        variables.put("teamId", teamId);
        if (description != null) variables.put("description", description);
        if (assigneeId != null) variables.put("assigneeId", assigneeId);
        if (stateId != null) variables.put("stateId", stateId);

        return executeGraphQL(context, query, variables);
    }

    @ActionMapping(appKey = "linear", actionKey = "linear:issue:delete")
    public Object deleteIssue(ActionContext context) throws Exception {
        String issueId = context.getString("issueId");
        String query = """
            mutation IssueDelete ($issueId: String!) {
                issueDelete(id: $issueId) {
                    success
                }
            }
        """;
        return executeGraphQL(context, query, Map.of("issueId", issueId));
    }

    @ActionMapping(appKey = "linear", actionKey = "linear:issue:get")
    public Object getIssue(ActionContext context) throws Exception {
        String issueId = context.getString("issueId");
        String query = """
            query Issue($issueId: String!) {
                issue(id: $issueId) {
                    id
                    identifier
                    title
                    description
                }
            }
        """;
        return executeGraphQL(context, query, Map.of("issueId", issueId));
    }

    @ActionMapping(appKey = "linear", actionKey = "linear:issue:getAll")
    public Object getAllIssues(ActionContext context) throws Exception {
        String query = """
            query Issue ($first: Int){
                issues (first: $first){
                    nodes {
                        id
                        identifier
                        title
                        description
                    }
                }
            }
        """;
        return executeGraphQL(context, query, Map.of("first", 50));
    }

    @ActionMapping(appKey = "linear", actionKey = "linear:issue:update")
    public Object updateIssue(ActionContext context) throws Exception {
        String issueId = context.getString("issueId");
        String title = context.getString("title");
        String description = context.getString("description");
        String assigneeId = context.getString("assigneeId");
        String stateId = context.getString("stateId");

        String query = """
            mutation IssueUpdate (
                $issueId: String!,
                $title: String,
                $description: String,
                $assigneeId: String,
                $stateId: String){
                issueUpdate(
                    id: $issueId,
                    input: {
                        title: $title
                        description: $description
                        assigneeId: $assigneeId
                        stateId: $stateId
                    }
                ) {
                    success
                    issue {
                        id
                        identifier
                        title
                    }
                }
            }
        """;

        Map<String, Object> variables = new HashMap<>();
        variables.put("issueId", issueId);
        if (title != null) variables.put("title", title);
        if (description != null) variables.put("description", description);
        if (assigneeId != null) variables.put("assigneeId", assigneeId);
        if (stateId != null) variables.put("stateId", stateId);

        return executeGraphQL(context, query, variables);
    }

    @ActionMapping(appKey = "linear", actionKey = "linear:issue:addLink")
    public Object addLink(ActionContext context) throws Exception {
        String issueId = context.getString("issueId");
        String link = context.getString("link");

        String query = """
            mutation AttachmentLinkURL($url: String!, $issueId: String!) {
                attachmentLinkURL(url: $url, issueId: $issueId) {
                    success
                }
            }
        """;
        return executeGraphQL(context, query, Map.of("issueId", issueId, "url", link));
    }

    // ─── COMMENT ───

    @ActionMapping(appKey = "linear", actionKey = "linear:comment:addComment")
    public Object addComment(ActionContext context) throws Exception {
        String issueId = context.getString("issueId");
        String comment = context.getString("comment");
        String parentId = context.getString("parentId");

        String query = """
            mutation CommentCreate ($issueId: String!, $body: String!, $parentId: String) {
                commentCreate(input: {issueId: $issueId, body: $body, parentId: $parentId}) {
                    success
                    comment {
                        id
                    }
                }
            }
        """;

        Map<String, Object> variables = new HashMap<>();
        variables.put("issueId", issueId);
        variables.put("body", comment);
        if (parentId != null) variables.put("parentId", parentId);

        return executeGraphQL(context, query, variables);
    }
}
