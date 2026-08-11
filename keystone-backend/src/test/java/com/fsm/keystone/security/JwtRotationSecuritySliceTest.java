package com.fsm.keystone.security;

import com.fsm.keystone.controller.WorkOrderController;
import com.fsm.keystone.fixtures.JwtTestKeys;
import com.fsm.keystone.service.WorkOrderService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @WebMvcTest slice verifying that the dual-key rotation window works at the HTTP layer.
 *
 * <p>Configures the slice with KEY_B as current and KEY_A as previous-secret, then
 * confirms that a token signed with the previous key reaches the controller, while a
 * token signed with an unrelated key returns a structured 401 with no stack trace.</p>
 */
@Tag("slice")
@WebMvcTest(WorkOrderController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origin=http://localhost:5173",
        "app.jwt.secret=" + JwtTestKeys.KEY_B,
        "app.jwt.previous-secret=" + JwtTestKeys.KEY_A,
        "app.jwt.expiration-ms=3600000"
})
class JwtRotationSecuritySliceTest {

    private static final String SUBJECT = "slice.manager@example.test";
    private static final String PROTECTED = "/api/work-orders";

    @Autowired
    MockMvc mockMvc;

    @MockBean AuthenticationProvider authenticationProvider;
    @MockBean UserDetailsService     userDetailsService;
    @MockBean WorkOrderService       workOrderService;

    @Test
    void previousKeyToken_withKid_reachesController() throws Exception {
        stubUser();
        when(workOrderService.getAllWorkOrders()).thenReturn(List.of());

        String prevToken = tokenSignedWith(JwtTestKeys.KEY_A, true);

        mockMvc.perform(get(PROTECTED).header("Authorization", "Bearer " + prevToken))
                .andExpect(status().isOk());
    }

    @Test
    void previousKeyToken_legacyNoKid_reachesController() throws Exception {
        stubUser();
        when(workOrderService.getAllWorkOrders()).thenReturn(List.of());

        String legacyToken = tokenSignedWith(JwtTestKeys.KEY_A, false);

        mockMvc.perform(get(PROTECTED).header("Authorization", "Bearer " + legacyToken))
                .andExpect(status().isOk());
    }

    @Test
    void currentKeyToken_reachesController() throws Exception {
        stubUser();
        when(workOrderService.getAllWorkOrders()).thenReturn(List.of());

        String currentToken = tokenSignedWith(JwtTestKeys.KEY_B, true);

        mockMvc.perform(get(PROTECTED).header("Authorization", "Bearer " + currentToken))
                .andExpect(status().isOk());
    }

    @Test
    void unknownKeyToken_returns401WithStructuredBody_noStackTrace() throws Exception {
        String unrelatedKey = "some-completely-different-secret-of-at-least-64-chars-00000000";
        // Include kid so the filter takes the deterministic rejection path (Unknown key identifier).
        String unknownToken = tokenSignedWith(unrelatedKey, true);

        mockMvc.perform(get(PROTECTED).header("Authorization", "Bearer " + unknownToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.code", is("AUTHENTICATION_FAILED")))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void malformedToken_returns401WithStructuredBody() throws Exception {
        mockMvc.perform(get(PROTECTED).header("Authorization", "Bearer not.a.valid.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.code", is("AUTHENTICATION_FAILED")));
    }

    private void stubUser() {
        UserDetails ud = User.withUsername(SUBJECT)
                .password("ignored")
                .roles("MANAGER")
                .build();
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(ud);
    }

    private String tokenSignedWith(String keyString, boolean includeKid) {
        SecretKey key = Keys.hmacShaKeyFor(keyString.getBytes(StandardCharsets.UTF_8));
        var builder = Jwts.builder();
        if (includeKid) {
            builder.header().keyId(JwtService.deriveKeyId(keyString)).and();
        }
        return builder
                .subject(SUBJECT)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(key)
                .compact();
    }
}
