package com.crescendo.apps.asana;

import com.crescendo.apps.simpleapi.SimpleApiSupport;
import com.crescendo.execution.action.*;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import java.util.*;

@ActionMapping(appKey = "asana", actionKey = "create-task")
class AsanaCreateTaskHandler implements ActionHandler {
    private final ObjectMapper m;

    AsanaCreateTaskHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("workspace", SimpleApiSupport.cfg(c, "workspace"));
            d.put("name", SimpleApiSupport.cfg(c, "name"));
            if (!SimpleApiSupport.cfg(c, "notes").isBlank()) {
                d.put("notes", SimpleApiSupport.cfg(c, "notes"));
            }
            if (!SimpleApiSupport.cfg(c, "projects").isBlank()) {
                d.put("projects", List.of(SimpleApiSupport.cfg(c, "projects").split(",")));
            }
            String res = SimpleApiSupport.bearer("https://app.asana.com/api/1.0", SimpleApiSupport.cred(c, "accessToken"))
                    .post()
                    .uri("/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("data", d))
                    .retrieve()
                    .body(String.class);
            return SimpleApiSupport.parsed(m, res);
        } catch (Exception e) {
            return ActionResult.failure("Asana create task failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "asana", actionKey = "list-tasks")
class AsanaListTasksHandler implements ActionHandler {
    private final ObjectMapper m;

    AsanaListTasksHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String res = SimpleApiSupport.bearer("https://app.asana.com/api/1.0", SimpleApiSupport.cred(c, "accessToken"))
                    .get()
                    .uri("/projects/{project}/tasks", SimpleApiSupport.cfg(c, "project"))
                    .retrieve()
                    .body(String.class);
            return SimpleApiSupport.parsed(m, res);
        } catch (Exception e) {
            return ActionResult.failure("Asana list tasks failed: " + e.getMessage());
        }
    }
}
