package com.fsm.keystone.security;

import com.fsm.keystone.fixtures.Personas;
import com.fsm.keystone.fixtures.TestJwtFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Container-backed integration test that confirms the MockMvc slice results in
 * {@link RouteRoleMatrixTest} are not an artifact of mocking.
 *
 * <p>Starts a full {@link SpringBootTest} application context with a real PostgreSQL 16
 * container, seeds the database via {@code seed-two-tenants.sql}, and issues real
 * {@link TestJwtFactory} bearer tokens for the scenario personas. The tokens are
 * validated by the production {@link JwtAuthenticationFilter} against the seeded users
 * loaded from the real {@link com.fsm.keystone.repository.UserRepository}, confirming
 * that the security slice accurately mirrors the deployed filter-chain behaviour.</p>
 *
 * <p>A representative subset of matrix rows is replayed — one authenticated and one
 * anonymous request per filter-chain category (permitAll and authenticated). Full
 * coverage remains in the slice suite; this test is a spot-check.</p>
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = {
        "app.cors.allowed-origin=http://localhost:5173",
        "app.jwt.secret=test-fixture-jwt-secret-key-must-be-at-least-64-chars-00000000",
        "app.jwt.expiration-ms=3600000",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.flyway.enabled=false"
})
@Sql(scripts = "/fixtures/seed-two-tenants.sql",
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class RouteRoleMatrixIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    MockMvc mockMvc;

    // ── permitAll endpoints — anonymous access ────────────────────────────────

    @Test
    void permitAll_getCustomers_allowsAnonymous() throws Exception {
        // /api/customers/** is permitAll → no authentication header → 200
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void permitAll_getUsers_allowsAnonymous() throws Exception {
        // /api/users/** is permitAll → 200
        mockMvc.perform(get("/api/users"))
                .andExpect(status().is2xxSuccessful());
    }

    // ── authenticated endpoints — anonymous access denied ────────────────────

    @Test
    void authenticated_getWorkOrders_deniesAnonymous() throws Exception {
        // /api/work-orders is anyRequest().authenticated() → 403 for no token
        mockMvc.perform(get("/api/work-orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticated_getParts_deniesAnonymous() throws Exception {
        // /api/parts is anyRequest().authenticated() → 403 for no token
        mockMvc.perform(get("/api/parts"))
                .andExpect(status().isForbidden());
    }

    // ── bearer token: MANAGER accesses authenticated endpoints ───────────────

    @Test
    void bearerToken_manager_getWorkOrders_returns200() throws Exception {
        // scenario.manager@example.test seeded in seed-two-tenants.sql (id=401, MANAGER)
        // TestJwtFactory uses the same signing key as the app — token validates against the
        // real filter chain loading the user from PostgreSQL.
        mockMvc.perform(get("/api/work-orders")
                        .header("Authorization",
                                TestJwtFactory.bearerHeaderFor(Personas.MANAGER)))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void bearerToken_technician_getParts_returns200() throws Exception {
        // scenario.technician@example.test seeded in seed-two-tenants.sql (id=403, TECHNICIAN)
        mockMvc.perform(get("/api/parts")
                        .header("Authorization",
                                TestJwtFactory.bearerHeaderFor(Personas.TECHNICIAN)))
                .andExpect(status().is2xxSuccessful());
    }

    // ── bearer token: TECHNICIAN blocked by @PreAuthorize ────────────────────

    @Test
    void bearerToken_technician_getDashboard_returns403() throws Exception {
        // /api/dashboard/summary requires hasAnyRole('MANAGER','DISPATCHER') → TECHNICIAN → 403
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization",
                                TestJwtFactory.bearerHeaderFor(Personas.TECHNICIAN)))
                .andExpect(status().isForbidden());
    }

    @Test
    void bearerToken_dispatcher_getDashboard_returns200() throws Exception {
        // scenario.dispatcher@example.test seeded (id=402, DISPATCHER) → 200
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization",
                                TestJwtFactory.bearerHeaderFor(Personas.DISPATCHER)))
                .andExpect(status().is2xxSuccessful());
    }
}
