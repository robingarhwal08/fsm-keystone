package com.fsm.keystone.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsm.keystone.dto.AuthResponse;
import com.fsm.keystone.dto.SignupRequest;
import com.fsm.keystone.enums.Role;
import com.fsm.keystone.fixtures.JwtTestKeys;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testcontainers integration test verifying the dual-key rotation window at the HTTP layer.
 *
 * <p>Configures the application with KEY_B as the current signing key and KEY_A as the
 * previous key. Signs up a user to obtain a KEY_B token (simulating a post-rotation login),
 * then manually generates a KEY_A token (simulating a pre-rotation session still alive).
 * Both tokens must authorise a protected endpoint. An unrelated-key token must be rejected
 * with 401. The fallback counter must increment for the pre-rotation token.</p>
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = {
        "app.cors.allowed-origin=http://localhost:5173",
        "app.jwt.secret=" + JwtTestKeys.KEY_B,
        "app.jwt.previous-secret=" + JwtTestKeys.KEY_A,
        "app.jwt.expiration-ms=3600000",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.flyway.enabled=false"
})
class JwtRotationWindowIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;
    @Autowired MeterRegistry meterRegistry;

    private static final String EMAIL    = "rotation.it.user@example.test";
    private static final String PASSWORD = "Rotation1tPassword!";

    @Test
    void rotationWindow_preAndPostRotationTokens_bothAuthorizeProtectedEndpoint() throws Exception {
        // Step 1: Sign up a MANAGER user → get a KEY_B (current) token from the response.
        SignupRequest signup = new SignupRequest(
                "Rotation IT User", EMAIL, PASSWORD, null, Role.MANAGER, null);
        MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signup)))
                .andExpect(status().isOk())
                .andReturn();
        AuthResponse signupResponse = objectMapper.readValue(
                signupResult.getResponse().getContentAsString(), AuthResponse.class);
        String postRotationToken = signupResponse.token(); // signed with KEY_B

        // Step 2: Call /api/auth/login to confirm a new login also returns a KEY_B token.
        String loginBody = objectMapper.writeValueAsString(
                new com.fsm.keystone.dto.AuthRequest(EMAIL, PASSWORD));
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();
        AuthResponse loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), AuthResponse.class);
        assertThat(loginResponse.token()).isNotBlank();
        // Both the signup and login tokens should carry the KEY_B kid.
        assertThat(JwtService.extractKidUnsafe(loginResponse.token()))
                .isEqualTo(JwtService.deriveKeyId(JwtTestKeys.KEY_B));

        // Step 3: Generate a pre-rotation KEY_A token (no kid — legacy format).
        String preRotationToken = buildLegacyToken(JwtTestKeys.KEY_A, EMAIL);

        // Step 4: Both tokens must reach the protected endpoint.
        mockMvc.perform(get("/api/work-orders")
                        .header("Authorization", "Bearer " + postRotationToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/work-orders")
                        .header("Authorization", "Bearer " + preRotationToken))
                .andExpect(status().isOk());

        // Step 5: Fallback counter must have been incremented for the pre-rotation token.
        double fallbacks = meterRegistry.find(JwtService.FALLBACK_COUNTER_NAME)
                .counters().stream()
                .mapToDouble(io.micrometer.core.instrument.Counter::count)
                .sum();
        assertThat(fallbacks).isGreaterThanOrEqualTo(1.0);

        // Step 6: An unrelated-key token must return 401.
        String unrelatedKey = "some-completely-different-secret-of-at-least-64-chars-00000000";
        String unknownToken = buildLegacyToken(unrelatedKey, EMAIL);
        mockMvc.perform(get("/api/work-orders")
                        .header("Authorization", "Bearer " + unknownToken))
                .andExpect(status().isUnauthorized());
    }

    private String buildLegacyToken(String keyString, String subject) {
        SecretKey key = Keys.hmacShaKeyFor(keyString.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(key)
                .compact();
    }
}
