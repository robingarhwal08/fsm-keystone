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

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that {@link JwtAuthenticationFilter} maps all JJWT exceptions to a structured
 * 401 response rather than propagating them as server errors.
 *
 * <p>These tests replace the characterization assertions that pinned the (now-fixed) 500
 * behaviour. The filter now catches {@link io.jsonwebtoken.JwtException} and
 * {@link IllegalArgumentException}, leaves the {@code SecurityContext} empty, and lets
 * Spring Security's {@code AuthenticationEntryPoint} return the standard error envelope.</p>
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

    @Test
    void malformedToken_returns401WithStructuredErrorBody() throws Exception {
        mockMvc.perform(get(PROTECTED_ENDPOINT)
                        .header("Authorization", "Bearer not.a.valid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.code", is("AUTHENTICATION_FAILED")))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.correlationId").isString());
    }

    @Test
    void expiredToken_returns401WithStructuredErrorBody() throws Exception {
        // Build an expired token signed with the test secret.
        io.jsonwebtoken.Jwts.Builder builder = io.jsonwebtoken.Jwts.builder()
                .subject("scenario.manager@example.test")
                .issuedAt(new java.util.Date(System.currentTimeMillis() - 7_200_000L))
                .expiration(new java.util.Date(System.currentTimeMillis() - 3_600_000L))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        com.fsm.keystone.fixtures.TestJwtFactory.TEST_SECRET.getBytes()));
        String expiredJwt = builder.compact();

        mockMvc.perform(get(PROTECTED_ENDPOINT)
                        .header("Authorization", "Bearer " + expiredJwt))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.code", is("AUTHENTICATION_FAILED")))
                .andExpect(jsonPath("$.message").isString());
    }
}
