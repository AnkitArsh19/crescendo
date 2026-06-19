package com.crescendo.apps.salesforce;

import com.crescendo.apps.simpleapi.SimpleApiSupport;
import com.crescendo.execution.action.*;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import java.util.*;

class SalesforceBase {
    static String base(ActionContext c) {
        return SimpleApiSupport.trim(SimpleApiSupport.cred(c, "instanceUrl")) + "/services/data/" +
                (SimpleApiSupport.cred(c, "apiVersion").isBlank() ? "v60.0" : SimpleApiSupport.cred(c, "apiVersion"));
    }
}

@ActionMapping(appKey = "salesforce", actionKey = "query")
class SalesforceQueryHandler implements ActionHandler {
    private final ObjectMapper m;

    SalesforceQueryHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            return SimpleApiSupport.parsed(m, SimpleApiSupport.bearer(SalesforceBase.base(c), SimpleApiSupport.cred(c, "accessToken"))
                    .get()
                    .uri("/query?q={q}", SimpleApiSupport.cfg(c, "soql"))
                    .retrieve()
                    .body(String.class));
        } catch (Exception e) {
            return ActionResult.failure("Salesforce query failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "salesforce", actionKey = "create-record")
class SalesforceCreateRecordHandler implements ActionHandler {
    private final ObjectMapper m;

    SalesforceCreateRecordHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            Object fields = c.configuration().get("fields");
            Object body = fields instanceof Map<?, ?> ? fields : m.readValue(String.valueOf(fields), Object.class);
            String res = SimpleApiSupport.bearer(SalesforceBase.base(c), SimpleApiSupport.cred(c, "accessToken"))
                    .post()
                    .uri("/sobjects/{object}", SimpleApiSupport.cfg(c, "object"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return SimpleApiSupport.parsed(m, res);
        } catch (Exception e) {
            return ActionResult.failure("Salesforce create record failed: " + e.getMessage());
        }
    }
}
