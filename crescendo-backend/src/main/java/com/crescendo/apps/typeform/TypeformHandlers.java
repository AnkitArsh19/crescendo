package com.crescendo.apps.typeform;

import com.crescendo.apps.simpleapi.SimpleApiSupport;
import com.crescendo.execution.action.*;
import tools.jackson.databind.ObjectMapper;

@ActionMapping(appKey = "typeform", actionKey = "list-forms")
class TypeformListFormsHandler implements ActionHandler {
    private final ObjectMapper m;

    TypeformListFormsHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            return SimpleApiSupport.parsed(m, SimpleApiSupport.bearer("https://api.typeform.com", SimpleApiSupport.cred(c, "accessToken"))
                    .get()
                    .uri("/forms")
                    .retrieve()
                    .body(String.class));
        } catch (Exception e) {
            return ActionResult.failure("Typeform list forms failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "typeform", actionKey = "list-responses")
class TypeformListResponsesHandler implements ActionHandler {
    private final ObjectMapper m;

    TypeformListResponsesHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            return SimpleApiSupport.parsed(m, SimpleApiSupport.bearer("https://api.typeform.com", SimpleApiSupport.cred(c, "accessToken"))
                    .get()
                    .uri("/forms/{form}/responses?page_size={size}",
                            SimpleApiSupport.cfg(c, "formId"),
                            Math.max(1, SimpleApiSupport.intCfg(c, "pageSize", 25)))
                    .retrieve()
                    .body(String.class));
        } catch (Exception e) {
            return ActionResult.failure("Typeform list responses failed: " + e.getMessage());
        }
    }
}
