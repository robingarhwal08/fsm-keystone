package com.fsm.keystone.exception;

import com.fsm.keystone.security.JwtAuthenticationFilter;
import com.fsm.keystone.security.JwtService;
import com.fsm.keystone.security.SecurityConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * WebMvc slice that drives every {@link GlobalExceptionHandler} branch via HTTP and asserts
 * the JSON schema and {@code X-Correlation-Id} response header.
 *
 * <p>Uses {@link ErrorTestController} as the purpose-built throwing controller.
 * {@link SecurityConfig}, {@link JwtAuthenticationFilter} and {@link JwtService} are imported
 * so the real filter chain runs, enabling filter-level 401/403 tests via the
 * {@code SecurityConfig} entry-point and denied-handler beans.</p>
 *
 * <p>Tests that must reach the controller use {@code @WithMockUser} to satisfy the
 * {@code anyRequest().authenticated()} rule. Tests verifying filter-chain 401/403
 * behaviour use {@code @WithAnonymousUser} or an insufficient-role mock user.</p>
 */
@Tag("slice")
@WebMvcTest(ErrorTestController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origin=http://localhost:5173",
        "app.jwt.secret=test-fixture-jwt-secret-key-must-be-at-least-64-chars-00000000",
        "app.jwt.expiration-ms=3600000"
})
@WithMockUser(roles = "MANAGER")
class GlobalExceptionHandlerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationProvider authenticationProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    // ── ApiException branches ─────────────────────────────────────────────────

    @Test
    void resourceNotFoundException_returns404WithExpectedShape() throws Exception {
        mockMvc.perform(get("/test/errors/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("WORK_ORDER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.correlationId").isString())
                .andExpect(jsonPath("$.path").value("/test/errors/not-found"))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void businessRuleException_returns409() throws Exception {
        mockMvc.perform(get("/test/errors/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"))
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void tenancyViolationException_returns403() throws Exception {
        mockMvc.perform(get("/test/errors/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("CROSS_TENANT_ACCESS_DENIED"))
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void authenticationFailedException_returns401() throws Exception {
        mockMvc.perform(get("/test/errors/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void rateLimitExceededException_returns429() throws Exception {
        mockMvc.perform(get("/test/errors/rate-limited"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(header().exists("X-Correlation-Id"));
    }

    // ── Bean Validation 400 ───────────────────────────────────────────────────

    @Test
    void methodArgumentNotValidException_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/test/errors/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_DATA"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.fieldErrors[*].field").exists())
                .andExpect(jsonPath("$.fieldErrors[*].message").exists())
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void methodArgumentNotValidException_passwordFieldMessage_usesGenericMask() throws Exception {
        // Submit with a password field — the error message for "password" must be masked.
        mockMvc.perform(post("/test/errors/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"\",\"fullName\":\"\",\"email\":\"\",\"role\":null}"))
                .andExpect(status().isBadRequest())
                // The fieldErrors for "password" must show "Value must satisfy constraints",
                // not any message that could echo the empty submitted value.
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'password')].message",
                        everyItem(equalTo("Value must satisfy constraints"))));
    }

    // ── Malformed body 400 ────────────────────────────────────────────────────

    @Test
    void httpMessageNotReadable_returns400WithSanitisedMessage() throws Exception {
        mockMvc.perform(post("/test/errors/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("this is not json { broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_DATA"))
                .andExpect(jsonPath("$.message").value("Malformed request body"))
                .andExpect(header().exists("X-Correlation-Id"));
    }

    // ── Data Integrity Violation 409 ──────────────────────────────────────────

    @Test
    void dataIntegrityViolation_returns409WithGenericMessage() throws Exception {
        mockMvc.perform(post("/test/errors/db-conflict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("DATA_INTEGRITY_VIOLATION"))
                .andExpect(jsonPath("$.message").value("The request conflicts with an existing resource"))
                .andExpect(header().exists("X-Correlation-Id"));
    }

    // ── Catch-all 500 ─────────────────────────────────────────────────────────

    @Test
    void unexpectedException_returns500WithGenericMessage() throws Exception {
        mockMvc.perform(get("/test/errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void unexpectedException_bodyDoesNotContainForbiddenSubstrings() throws Exception {
        String responseBody = mockMvc.perform(get("/test/errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // A10 policy: no class names, stack traces or internal details in 5xx response body.
        org.junit.jupiter.api.Assertions.assertFalse(
                responseBody.contains("com.fsm"),
                "Response body must not contain package names. Body: " + responseBody);
        org.junit.jupiter.api.Assertions.assertFalse(
                responseBody.contains("Exception"),
                "Response body must not contain 'Exception'. Body: " + responseBody);
        org.junit.jupiter.api.Assertions.assertFalse(
                responseBody.contains("at java"),
                "Response body must not contain stack-trace fragments. Body: " + responseBody);
    }

    // ── Filter-chain level 401 (SecurityConfig AuthenticationEntryPoint) ──────

    @Test
    @WithAnonymousUser
    void anonymousRequest_toProtectedEndpoint_returns401ViaEntryPoint() throws Exception {
        // Anonymous user accessing a protected endpoint triggers SecurityConfig.AuthenticationEntryPoint.
        // The response must be the canonical ApiErrorResponse, not Spring's default error page.
        mockMvc.perform(get("/test/errors/not-found"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.correlationId").isString())
                .andExpect(header().exists("X-Correlation-Id"));
    }

    // ── JSON schema: all required fields always present ───────────────────────

    @Test
    void everyErrorResponse_hasAllRequiredSchemaFields() throws Exception {
        mockMvc.perform(get("/test/errors/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.status").isNumber())
                .andExpect(jsonPath("$.code").isString())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.correlationId").isString())
                .andExpect(jsonPath("$.path").isString())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void correlationIdInBodyMatchesXCorrelationIdHeader() throws Exception {
        var result = mockMvc.perform(get("/test/errors/not-found"))
                .andExpect(status().isNotFound())
                .andReturn();

        String headerValue = result.getResponse().getHeader("X-Correlation-Id");
        String bodyJson = result.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertNotNull(headerValue,
                "X-Correlation-Id header must be present");
        org.junit.jupiter.api.Assertions.assertTrue(
                bodyJson.contains("\"correlationId\":\"" + headerValue + "\""),
                "correlationId in body must match X-Correlation-Id header");
    }
}
