package com.crescendo.workflow;

import com.crescendo.BaseIntegrationTest;
import com.crescendo.auth.dto.AuthDto;
import com.crescendo.security.access.AccessControlService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

/**
 * E2E integration tests for WorkflowController (/workflows/**).
 *
 * Authenticates via the real /auth endpoints to obtain a JWT, then exercises
 * all workflow CRUD and lifecycle endpoints over a real HTTP port.
 *
 * Spring Boot 4.0 removed the webmvc test slice (AutoConfigureMockMvc /
 * TestRestTemplate) so we use a plain RestTemplate with @LocalServerPort.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkflowControllerE2ETest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccessControlService accessControlService;

    private String accessToken;

    private static final String EMAIL    = "workflow-e2e@crescendo.test";
    private static final String USERNAME = "workflowe2e";
    private static final String PASSWORD = "P@ssword123!";

    // ── helpers ──────────────────────────────────────────────────────────────

    /** RestTemplate that never throws on 4xx/5xx — we assert on status codes ourselves. */
    private RestTemplate client() {
        RestTemplate rt = new RestTemplate();
        rt.setErrorHandler(new org.springframework.web.client.DefaultResponseErrorHandler() {
            @Override public boolean hasError(org.springframework.http.client.ClientHttpResponse r) { return false; }
        });
        return rt;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpEntity<Object> json(Object body) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, h);
    }

    private HttpEntity<Object> auth(Object body) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(accessToken);
        return new HttpEntity<>(body, h);
    }

    private HttpEntity<Void> authNoBody() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(accessToken);
        return new HttpEntity<>(null, h);
    }

    private String createWorkflowId(String name) throws Exception {
        String body = "{\"name\":\"" + name + "\",\"description\":\"E2E test\"}";
        ResponseEntity<String> resp = client().postForEntity(url("/workflows"), auth(body), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return objectMapper.readTree(resp.getBody()).get("id").asText();
    }

    @BeforeEach
    void authenticate() throws Exception {
        doNothing().when(accessControlService).enforceWorkflowLimit(any());

        // Register (409 on duplicate is fine — test user may persist across test methods in same context)
        client().postForEntity(url("/auth/register"),
                json(new AuthDto.RegisterRequest(EMAIL, USERNAME, PASSWORD, null, null)), String.class);

        ResponseEntity<String> loginResp = client().postForEntity(url("/auth/login"),
                json(new AuthDto.LoginRequest(EMAIL, PASSWORD, false, null, null)), String.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        accessToken = objectMapper.readTree(loginResp.getBody()).get("accessToken").asText();
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /workflows — Create")
    class Create {

        @Test
        @DisplayName("201 Created with workflow summary when authenticated")
        void create_validRequest_returns201() throws Exception {
            ResponseEntity<String> resp = client().postForEntity(url("/workflows"),
                    auth("{\"name\":\"My Workflow\",\"description\":\"Test\"}"), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            JsonNode json = objectMapper.readTree(resp.getBody());
            assertThat(json.get("id").asText()).isNotBlank();
            assertThat(json.get("name").asText()).isEqualTo("My Workflow");
        }

        @Test
        @DisplayName("401 Unauthorized when no Bearer token provided")
        void create_unauthenticated_returns401() {
            ResponseEntity<String> resp = client().postForEntity(url("/workflows"),
                    json("{\"name\":\"Unauthorized\",\"description\":\"Fail\"}"), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("400 Bad Request when name is blank")
        void create_blankName_returns400() {
            ResponseEntity<String> resp = client().postForEntity(url("/workflows"),
                    auth("{\"name\":\"\",\"description\":\"No name\"}"), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("GET /workflows — List")
    class ListWorkflows {

        @Test
        @DisplayName("200 OK returns a JSON array")
        void list_authenticated_returns200Array() {
            ResponseEntity<String> resp = client().exchange(
                    url("/workflows"), HttpMethod.GET, authNoBody(), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody()).startsWith("[");
        }

        @Test
        @DisplayName("200 OK contains created workflow name")
        void list_afterCreating_containsWorkflow() throws Exception {
            createWorkflowId("Listed Workflow");

            ResponseEntity<String> resp = client().exchange(
                    url("/workflows"), HttpMethod.GET, authNoBody(), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody()).contains("Listed Workflow");
        }
    }

    @Nested
    @DisplayName("GET /workflows/{id} — Detail")
    class GetDetail {

        @Test
        @DisplayName("200 OK with workflow detail for own workflow")
        void get_ownWorkflow_returns200() throws Exception {
            String id = createWorkflowId("Detail Workflow");

            ResponseEntity<String> resp = client().exchange(
                    url("/workflows/" + id), HttpMethod.GET, authNoBody(), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(objectMapper.readTree(resp.getBody()).get("id").asText()).isEqualTo(id);
        }

        @Test
        @DisplayName("404 Not Found for non-existent workflow ID")
        void get_nonExistentId_returns404() {
            ResponseEntity<String> resp = client().exchange(
                    url("/workflows/00000000-0000-0000-0000-000000000000"),
                    HttpMethod.GET, authNoBody(), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("PATCH /workflows/{id} — Update")
    class UpdateWorkflow {

        @Test
        @DisplayName("204 No Content and persists update")
        void update_ownWorkflow_returns204AndPersists() throws Exception {
            String id = createWorkflowId("Original Name");

            ResponseEntity<String> patch = client().exchange(
                    url("/workflows/" + id), HttpMethod.PATCH,
                    auth("{\"name\":\"Updated Name\",\"description\":\"Updated\"}"), String.class);
            assertThat(patch.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            ResponseEntity<String> get = client().exchange(
                    url("/workflows/" + id), HttpMethod.GET, authNoBody(), String.class);
            assertThat(objectMapper.readTree(get.getBody()).get("name").asText()).isEqualTo("Updated Name");
        }
    }

    @Nested
    @DisplayName("POST /workflows/{id}/activate & /deactivate — Lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("204 No Content on activate")
        void activate_returns204() throws Exception {
            String id = createWorkflowId("Activate Me");
            ResponseEntity<String> resp = client().postForEntity(
                    url("/workflows/" + id + "/activate"), authNoBody(), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @Test
        @DisplayName("204 No Content on deactivate")
        void deactivate_returns204() throws Exception {
            String id = createWorkflowId("Deactivate Me");
            ResponseEntity<String> resp = client().postForEntity(
                    url("/workflows/" + id + "/deactivate"), authNoBody(), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
    }

    @Nested
    @DisplayName("DELETE /workflows/{id} — Delete")
    class DeleteWorkflow {

        @Test
        @DisplayName("204 No Content on soft-delete")
        void delete_ownWorkflow_returns204() throws Exception {
            String id = createWorkflowId("Delete Me");
            ResponseEntity<String> resp = client().exchange(
                    url("/workflows/" + id), HttpMethod.DELETE, authNoBody(), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @Test
        @DisplayName("404 Not Found when deleting an already-deleted workflow")
        void delete_alreadyDeleted_returns404() throws Exception {
            String id = createWorkflowId("Delete Twice");

            client().exchange(url("/workflows/" + id), HttpMethod.DELETE, authNoBody(), String.class);

            ResponseEntity<String> second = client().exchange(
                    url("/workflows/" + id), HttpMethod.DELETE, authNoBody(), String.class);
            assertThat(second.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("POST /workflows/bulk — Bulk Operations")
    class BulkOps {

        @Test
        @DisplayName("204 No Content for bulk activate")
        void bulkActivate_returns204() throws Exception {
            String id1 = createWorkflowId("Bulk 1");
            String id2 = createWorkflowId("Bulk 2");
            String body = "{\"ids\":[\"" + id1 + "\",\"" + id2 + "\"]}";

            ResponseEntity<String> resp = client().postForEntity(
                    url("/workflows/bulk/activate"), auth(body), String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @Test
        @DisplayName("204 No Content for bulk deactivate")
        void bulkDeactivate_returns204() throws Exception {
            String id1 = createWorkflowId("BulkDeact 1");
            String id2 = createWorkflowId("BulkDeact 2");
            String body = "{\"ids\":[\"" + id1 + "\",\"" + id2 + "\"]}";

            ResponseEntity<String> resp = client().postForEntity(
                    url("/workflows/bulk/deactivate"), auth(body), String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
    }
}
