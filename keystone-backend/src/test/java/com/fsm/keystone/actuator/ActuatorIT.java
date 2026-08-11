package com.fsm.keystone.actuator;

import com.fsm.keystone.fixtures.Personas;
import com.fsm.keystone.fixtures.TestJwtFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Integration tests asserting the Actuator observability plane is correctly wired.
 *
 * <p>Covers:</p>
 * <ul>
 *   <li>Anonymous access to liveness and readiness probes → 200 UP</li>
 *   <li>Anonymous access to {@code /actuator/prometheus} → 401 (protected)</li>
 *   <li>Authenticated access to {@code /actuator/prometheus} → 200 with expected metric names</li>
 *   <li>Non-exposed endpoints ({@code /actuator/env}, {@code /actuator/beans},
 *       {@code /actuator/heapdump}) → 404</li>
 * </ul>
 *
 * <p>{@link UserDetailsService} is mocked so test JWTs from {@link TestJwtFactory} validate
 * without requiring real database rows for the test persona.</p>
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "app.jwt.secret=test-fixture-jwt-secret-key-must-be-at-least-64-chars-00000000",
        "app.jwt.expiration-ms=3600000",
        "app.cors.allowed-origin=http://localhost:5173"
})
@Testcontainers
class ActuatorIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @MockBean
    private UserDetailsService userDetailsService;

    @BeforeEach
    void stubUserService() {
        when(userDetailsService.loadUserByUsername(Personas.MANAGER.getEmail()))
                .thenReturn(Personas.MANAGER);
    }

    // ── Anonymous health probe access ─────────────────────────────────────────

    @Test
    void healthEndpoint_anonymousAccess_returns200() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/actuator/health"), String.class);
        assertEquals(200, response.getStatusCode().value(),
                "GET /actuator/health must be reachable without authentication");
    }

    @Test
    void livenessProbe_anonymousAccess_returns200WithStatusUp() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/actuator/health/liveness"), String.class);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"status\":\"UP\""),
                "Liveness probe must report UP. Body: " + response.getBody());
    }

    @Test
    void readinessProbe_anonymousAccess_returns200WithStatusUp() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/actuator/health/readiness"), String.class);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"status\":\"UP\""),
                "Readiness probe must report UP when database is available. Body: " + response.getBody());
    }

    // ── Prometheus endpoint security ──────────────────────────────────────────

    @Test
    void prometheusEndpoint_anonymousAccess_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/actuator/prometheus"), String.class);
        assertEquals(401, response.getStatusCode().value(),
                "/actuator/prometheus must require authentication");
    }

    @Test
    void prometheusEndpoint_authenticated_returns200WithExpectedMetricNames() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", TestJwtFactory.bearerHeaderFor(Personas.MANAGER));
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url("/actuator/prometheus"), HttpMethod.GET, entity, String.class);

        assertEquals(200, response.getStatusCode().value(),
                "/actuator/prometheus must return 200 for an authenticated user");
        assertNotNull(response.getBody());

        List<String> expectedNames = loadExpectedMetricNames();
        for (String metricName : expectedNames) {
            assertTrue(response.getBody().contains(metricName),
                    "Expected metric '" + metricName + "' not found in Prometheus scrape output");
        }
    }

    // ── Non-exposed endpoints must return 404 ────────────────────────────────

    @Test
    void envEndpoint_notExposed_returns404() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", TestJwtFactory.bearerHeaderFor(Personas.MANAGER));
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url("/actuator/env"), HttpMethod.GET, entity, String.class);
        assertEquals(404, response.getStatusCode().value(),
                "/actuator/env must not be exposed");
    }

    @Test
    void beansEndpoint_notExposed_returns404() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", TestJwtFactory.bearerHeaderFor(Personas.MANAGER));
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url("/actuator/beans"), HttpMethod.GET, entity, String.class);
        assertEquals(404, response.getStatusCode().value(),
                "/actuator/beans must not be exposed");
    }

    @Test
    void heapdumpEndpoint_notExposed_returns404() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", TestJwtFactory.bearerHeaderFor(Personas.MANAGER));
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url("/actuator/heapdump"), HttpMethod.GET, entity, String.class);
        assertEquals(404, response.getStatusCode().value(),
                "/actuator/heapdump must not be exposed");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private List<String> loadExpectedMetricNames() throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(
                        getClass().getClassLoader().getResourceAsStream(
                                "fixtures/metrics/expected-metric-names.txt"),
                        "expected-metric-names.txt not found on classpath")))) {
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .collect(Collectors.toList());
        }
    }
}
