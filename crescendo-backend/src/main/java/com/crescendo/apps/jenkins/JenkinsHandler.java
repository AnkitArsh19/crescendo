package com.crescendo.apps.jenkins;

import com.crescendo.execution.action.*;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;
import java.nio.charset.StandardCharsets;
import java.util.*;

abstract class JenkinsHandler implements ActionHandler {
    final ObjectMapper mapper;

    JenkinsHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    RestClient client(ActionContext c) {
        String auth = cred(c, "username") + ":" + cred(c, "apiToken");
        return RestClient.builder()
                .baseUrl(trim(cred(c, "baseUrl")))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8)))
                .build();
    }

    String path(ActionContext c) {
        String[] parts = val(c, "jobPath").split("/");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isBlank()) {
                sb.append("/job/").append(p);
            }
        }
        return sb.toString();
    }

    Object json(Object v, Object f) throws Exception {
        if (v == null) return f;
        if (v instanceof Map<?, ?> || v instanceof List<?>) return v;
        return mapper.readValue(String.valueOf(v), Object.class);
    }

    String val(ActionContext c, String k) {
        Object v = c.configuration().get(k);
        return v == null ? "" : String.valueOf(v);
    }

    String cred(ActionContext c, String k) {
        Object v = c.credentials() != null ? c.credentials().get(k) : null;
        return v == null ? "" : String.valueOf(v);
    }

    String trim(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
