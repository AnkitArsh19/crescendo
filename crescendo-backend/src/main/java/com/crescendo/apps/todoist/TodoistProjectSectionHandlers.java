package com.crescendo.apps.todoist;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Todoist Project and Section handlers.
 */
@Component
public class TodoistProjectSectionHandlers {

    private String getBaseUrl() {
        return "https://api.todoist.com/rest/v2";
    }

    private String getSyncBaseUrl() {
        return "https://api.todoist.com/sync/v9";
    }

    private String getAuth(ActionContext context) {
        return "Bearer " + context.getCredential("apiKey");
    }

    // ─── PROJECT ───

    @ActionMapping(appKey = "todoist", actionKey = "todoist:project:create")
    public Object createProject(ActionContext context) throws Exception {
        String name = context.getString("name");
        return RestClient.builder()
                .url(getBaseUrl() + "/projects")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("name", name))
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:project:get")
    public Object getProject(ActionContext context) throws Exception {
        String projectId = context.getString("projectId");
        return RestClient.builder()
                .url(getBaseUrl() + "/projects/" + projectId)
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:project:getAll")
    public Object getAllProjects(ActionContext context) throws Exception {
        return RestClient.builder()
                .url(getBaseUrl() + "/projects")
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:project:update")
    public Object updateProject(ActionContext context) throws Exception {
        String projectId = context.getString("projectId");
        String name = context.getString("name");
        return RestClient.builder()
                .url(getBaseUrl() + "/projects/" + projectId)
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("name", name))
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:project:delete")
    public Object deleteProject(ActionContext context) throws Exception {
        String projectId = context.getString("projectId");
        return RestClient.builder()
                .url(getBaseUrl() + "/projects/" + projectId)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:project:getCollaborators")
    public Object getCollaborators(ActionContext context) throws Exception {
        String projectId = context.getString("projectId");
        return RestClient.builder()
                .url(getBaseUrl() + "/projects/" + projectId + "/collaborators")
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:project:archive")
    public Object archiveProject(ActionContext context) throws Exception {
        String projectId = context.getString("projectId");
        Map<String, Object> command = new HashMap<>();
        command.put("type", "project_archive");
        command.put("uuid", java.util.UUID.randomUUID().toString());
        command.put("args", Map.of("id", projectId));

        return RestClient.builder()
                .url(getSyncBaseUrl() + "/sync")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("commands", java.util.List.of(command)))
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:project:unarchive")
    public Object unarchiveProject(ActionContext context) throws Exception {
        String projectId = context.getString("projectId");
        Map<String, Object> command = new HashMap<>();
        command.put("type", "project_unarchive");
        command.put("uuid", java.util.UUID.randomUUID().toString());
        command.put("args", Map.of("id", projectId));

        return RestClient.builder()
                .url(getSyncBaseUrl() + "/sync")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("commands", java.util.List.of(command)))
                .execute();
    }

    // ─── SECTION ───

    @ActionMapping(appKey = "todoist", actionKey = "todoist:section:create")
    public Object createSection(ActionContext context) throws Exception {
        String projectId = context.getString("projectId");
        String name = context.getString("name");
        return RestClient.builder()
                .url(getBaseUrl() + "/sections")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("project_id", projectId, "name", name))
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:section:get")
    public Object getSection(ActionContext context) throws Exception {
        String sectionId = context.getString("sectionId");
        return RestClient.builder()
                .url(getBaseUrl() + "/sections/" + sectionId)
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:section:getAll")
    public Object getAllSections(ActionContext context) throws Exception {
        String projectId = context.getString("projectId");
        String url = getBaseUrl() + "/sections";
        if (projectId != null && !projectId.isBlank()) {
            url += "?project_id=" + projectId;
        }
        return RestClient.builder()
                .url(url)
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:section:update")
    public Object updateSection(ActionContext context) throws Exception {
        String sectionId = context.getString("sectionId");
        String name = context.getString("name");
        return RestClient.builder()
                .url(getBaseUrl() + "/sections/" + sectionId)
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("name", name))
                .execute();
    }

    @ActionMapping(appKey = "todoist", actionKey = "todoist:section:delete")
    public Object deleteSection(ActionContext context) throws Exception {
        String sectionId = context.getString("sectionId");
        return RestClient.builder()
                .url(getBaseUrl() + "/sections/" + sectionId)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }
}
