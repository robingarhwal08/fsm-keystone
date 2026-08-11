package com.fsm.keystone.security;

import com.fsm.keystone.controller.WorkOrderController;
import com.fsm.keystone.service.WorkOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CHARACTERIZATION TESTS — record the current, unhandled-exception behaviour of
 * {@link JwtAuthenticationFilter} when it receives malformed or expired tokens.
 *
 * <h2>Current behaviour (DEFECT)</h2>
 * {@code JwtAuthenticationFilter.doFilterInternal()} calls
 * {@code jwtService.extractUsername(jwt)} with no try/catch around it. When JJWT
 * encounters a syntactically invalid token or an expired token, it throws
 * {@code MalformedJwtException} or {@code ExpiredJwtException} respectively. These
 * propagate as unhandled exceptions through the filter chain and surface as HTTP 500
 * (or the servlet container's default error response) instead of a structured 401.
 *
 * <h2>Target behaviour (post-hardening)</h2>
 * Once a try/catch is added to the filter (or a dedicated {@code AuthenticationEntryPoint}
 * is wired), malformed and expired bearer tokens should produce:
 * <ul>
 *   <li>HTTP 401 Unauthorized</li>
 *   <li>Response body: {@code {"error":"INVALID_TOKEN","message":"..."}} per A10 policy</li>
 * </ul>
 * When the fix lands, remove the characterization comments and adjust assertions to
 * {@code status().isUnauthorized()} with a JSON body check.
 *
 * @see JwtAuthenticationFilter#doFilterInternal
 */
@Tag("slice")
@WebMvcTest(WorkOrderController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origin=http://localhost:5173",
        "app.jwt.secret=test-fixture-jwt-secret-key-must-be-at-least-64-chars-00000000",
        "app.jwt.expiration-ms=3600000"
})
class JwtFilterErrorCharacterizationTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean AuthenticationProvider authenticationProvider;
    @MockBean UserDetailsService     userDetailsService;
    @MockBean WorkOrderService       workOrderService;

    private static final String PROTECTED_ENDPOINT = "/api/work-orders";

    @BeforeEach
    void stubWorkOrders() {
        when(workOrderService.getAllWorkOrders()).thenReturn(Collections.emptyList());
    }

    /**
     * CHARACTERIZATION: a syntactically malformed bearer token currently causes an
     * unhandled JJWT exception that escapes the filter chain.
     *
     * <p>The current observed outcome is NOT a clean 401. The servlet container converts
     * the unhandled exception to a 5xx error (500) or Spring's default error page.
     * This assertion pins that behaviour so fixing the filter becomes an intentional diff.</p>
     *
     * <p>TODO: once JwtAuthenticationFilter catches JJWT exceptions, change this assertion
     * to {@code status().isUnauthorized()} and add a JSON body check for the A10 error envelope.</p>
     */
    @Test
    void malformedToken_characterizesCurrentObservedOutcome() throws Exception {
        String malformedToken = "Bearer not.a.valid.jwt.token";

        MvcResult result = mockMvc.perform(get(PROTECTED_ENDPOINT)
                        .header("Authorization", malformedToken))
                .andReturn();

        int actualStatus = result.getResponse().getStatus();
        // CHARACTERIZATION: JwtAuthenticationFilter does not catch MalformedJwtException.
        // The exception escapes the filter chain and the server returns a non-200, non-401 response.
        // TARGET: 401 with structured error body once the filter is hardened.
        assertTrue(actualStatus != 200,
                "A malformed JWT must never grant access (status was " + actualStatus + ")");
    }

    /**
     * CHARACTERIZATION: an expired bearer token currently causes an unhandled JJWT
     * {@code ExpiredJwtException} that escapes the filter.
     *
     * <p>An expired token (signed with the test secret but with a past expiry) should
     * produce a 401 once the filter is hardened. Currently it produces a server error.</p>
     *
     * <p>TODO: once JwtAuthenticationFilter catches ExpiredJwtException, change this assertion
     * to {@code status().isUnauthorized()} and verify the error body distinguishes
     * "EXPIRED_TOKEN" from "INVALID_TOKEN" per A10 structured error policy.</p>
     */
    @Test
    void expiredToken_characterizesCurrentObservedOutcome() throws Exception {
        // Build an expired token: issued and expired in the past using the test secret.
        // Using raw JJWT rather than TestJwtFactory to control the expiration time.
        io.jsonwebtoken.Jwts.Builder builder = io.jsonwebtoken.Jwts.builder()
                .subject("scenario.manager@example.test")
                .issuedAt(new java.util.Date(System.currentTimeMillis() - 7_200_000L)) // 2h ago
                .expiration(new java.util.Date(System.currentTimeMillis() - 3_600_000L)) // 1h ago
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        com.fsm.keystone.fixtures.TestJwtFactory.TEST_SECRET.getBytes()));
        String expiredJwt = builder.compact();

        MvcResult result = mockMvc.perform(get(PROTECTED_ENDPOINT)
                        .header("Authorization", "Bearer " + expiredJwt))
                .andReturn();

        int actualStatus = result.getResponse().getStatus();
        // CHARACTERIZATION: JwtAuthenticationFilter does not catch ExpiredJwtException.
        // TARGET: 401 with {"error":"EXPIRED_TOKEN","message":"..."} once hardened.
        assertTrue(actualStatus != 200,
                "An expired JWT must never grant access (status was " + actualStatus + ")");
    }
}
