package com.crescendo.apps.clickup;

import com.crescendo.apps.simpleapi.SimpleApiSupport;
import com.crescendo.execution.action.*;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;
import java.util.*;

class ClickUpBase {
    static RestClient c(ActionContext x) {
        return RestClient.builder()
                .baseUrl("https://api.clickup.com/api/v2")
                .defaultHeader("Authorization", SimpleApiSupport.cred(x, "apiToken"))
                .build();
    }
}

@ActionMapping(appKey = "clickup", actionKey = "create-task")
class ClickUpCreateTaskHandler implements ActionHandler {
    private final ObjectMapper m;

    ClickUpCreateTaskHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String res = ClickUpBase.c(c).post()
                    .uri("/list/{list}/task", SimpleApiSupport.cfg(c, "listId"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "name", SimpleApiSupport.cfg(c, "name"),
                            "description", SimpleApiSupport.cfg(c, "description")
                    ))
                    .retrieve()
                    .body(String.class);
            return SimpleApiSupport.parsed(m, res);
        } catch (Exception e) {
            return ActionResult.failure("ClickUp create task failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "clickup", actionKey = "list-tasks")
class ClickUpListTasksHandler implements ActionHandler {
    private final ObjectMapper m;

    ClickUpListTasksHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String res = ClickUpBase.c(c).get()
                    .uri("/list/{list}/task", SimpleApiSupport.cfg(c, "listId"))
                    .retrieve()
                    .body(String.class);
            return SimpleApiSupport.parsed(m, res);
        } catch (Exception e) {
            return ActionResult.failure("ClickUp list tasks failed: " + e.getMessage());
        }
    }
}
