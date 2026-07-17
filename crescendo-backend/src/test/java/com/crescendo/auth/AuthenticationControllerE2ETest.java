package com.crescendo.auth;

import com.crescendo.BaseIntegrationTest;
import com.crescendo.auth.dto.AuthDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * E2E integration tests for AuthenticationController (/auth/**).
 *
 * Uses RestTemplate over a real HTTP port with Testcontainers-backed
 * Postgres and Redis (provided by BaseIntegrationTest).
 *
 * Spring Boot 4.0 removed the webmvc test slice (AutoConfigureMockMvc /
 * TestRestTemplate), so we wire a plain RestTemplate manually.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthenticationControllerE2ETest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    // ── helpers ──────────────────────────────────────────────────────────────

    private RestTemplate client() {
        // RestTemplate that does NOT throw on 4xx/5xx so we can assert on the status code.
        RestTemplate rt = new RestTemplate();
        rt.setErrorHandler(new org.springframework.web.client.DefaultResponseErrorHandler() {
            @Override public boolean hasError(org.springframework.http.client.ClientHttpResponse r) { return false; }
        });
        return rt;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private AuthDto.RegisterRequest reg(String email, String username, String password) {
        return new AuthDto.RegisterRequest(email, username, password, null, null);
    }

    private AuthDto.LoginRequest login(String email, String password) {
        return new AuthDto.LoginRequest(email, password, false, null, null);
    }

    private HttpEntity<Object> json(Object body) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, h);
    }

    private HttpEntity<Object> jsonBearer(Object body, String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return new HttpEntity<>(body, h);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/register")
    class Register {

        @Test
        @DisplayName("201 Created with tokens when valid credentials provided")
        void register_validRequest_returns201WithTokens() throws Exception {
            ResponseEntity<String> resp = client().postForEntity(
                    url("/auth/register"), json(reg("reg-ok@crescendo.test", "regok", "P@ssword123!")), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            JsonNode body = objectMapper.readTree(resp.getBody());
            assertThat(body.get("accessToken").asText()).isNotBlank();
            assertThat(body.get("refreshToken").asText()).isNotBlank();
        }

        @Test
        @DisplayName("409 Conflict when email is already registered")
        void register_duplicateEmail_returns409() {
            client().postForEntity(url("/auth/register"),
                    json(reg("dup@crescendo.test", "dup1", "P@ssword123!")), String.class);

            ResponseEntity<String> second = client().postForEntity(url("/auth/register"),
                    json(reg("dup@crescendo.test", "dup2", "P@ssword123!")), String.class);

            assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("400 Bad Request when required fields are missing")
        void register_missingPassword_returns400() {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> resp = client().postForEntity(url("/auth/register"),
                    new HttpEntity<>("{\"email\":\"nopass@test.com\",\"username\":\"nopass\"}", h), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("POST /auth/login")
    class Login {

        private void registerUser(String email, String username) {
            client().postForEntity(url("/auth/register"),
                    json(reg(email, username, "P@ssword123!")), String.class);
        }

        @Test
        @DisplayName("200 OK with access token on valid credentials")
        void login_validCredentials_returns200() throws Exception {
            registerUser("login-ok@crescendo.test", "loginok");

            ResponseEntity<String> resp = client().postForEntity(url("/auth/login"),
                    json(login("login-ok@crescendo.test", "P@ssword123!")), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(objectMapper.readTree(resp.getBody()).get("accessToken").asText()).isNotBlank();
        }

        @Test
        @DisplayName("401 Unauthorized on wrong password")
        void login_wrongPassword_returns401() {
            registerUser("wrongpass@crescendo.test", "wrongpass");

            ResponseEntity<String> resp = client().postForEntity(url("/auth/login"),
                    json(login("wrongpass@crescendo.test", "WrongPassword!")), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("401 Unauthorized on non-existent email")
        void login_unknownEmail_returns401() {
            ResponseEntity<String> resp = client().postForEntity(url("/auth/login"),
                    json(login("nobody@crescendo.test", "P@ssword123!")), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("POST /auth/refresh")
    class Refresh {

        @Test
        @DisplayName("200 OK with new access token using valid refresh token")
        void refresh_validToken_returns200() throws Exception {
            ResponseEntity<String> regResp = client().postForEntity(url("/auth/register"),
                    json(reg("refresh@crescendo.test", "refresher", "P@ssword123!")), String.class);
            String refreshToken = objectMapper.readTree(regResp.getBody()).get("refreshToken").asText();

            ResponseEntity<String> resp = client().postForEntity(url("/auth/refresh"),
                    json(new AuthDto.RefreshTokenRequest(refreshToken)), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(objectMapper.readTree(resp.getBody()).get("accessToken").asText()).isNotBlank();
        }

        @Test
        @DisplayName("400 Bad Request when no refresh token provided")
        void refresh_noBody_returns400() {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> resp = client().postForEntity(url("/auth/refresh"),
                    new HttpEntity<>(null, h), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("POST /auth/forgot-password")
    class ForgotPassword {

        @Test
        @DisplayName("204 No Content even for unregistered email (prevents enumeration)")
        void forgotPassword_unknownEmail_returns204() {
            ResponseEntity<String> resp = client().postForEntity(url("/auth/forgot-password"),
                    json(new AuthDto.ForgotPasswordRequest("notexist@crescendo.test")), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @Test
        @DisplayName("204 No Content for registered email")
        void forgotPassword_knownEmail_returns204() {
            client().postForEntity(url("/auth/register"),
                    json(reg("forgot@crescendo.test", "forgotuser", "P@ssword123!")), String.class);

            ResponseEntity<String> resp = client().postForEntity(url("/auth/forgot-password"),
                    json(new AuthDto.ForgotPasswordRequest("forgot@crescendo.test")), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
    }

    @Nested
    @DisplayName("POST /auth/logout")
    class Logout {

        @Test
        @DisplayName("400 Bad Request when no refresh cookie present")
        void logout_noRefreshCookie_returns400() throws Exception {
            ResponseEntity<String> regResp = client().postForEntity(url("/auth/register"),
                    json(reg("logout@crescendo.test", "logoutuser", "P@ssword123!")), String.class);
            String accessToken = objectMapper.readTree(regResp.getBody()).get("accessToken").asText();

            ResponseEntity<String> resp = client().postForEntity(url("/auth/logout"),
                    jsonBearer(null, accessToken), String.class);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }
}
