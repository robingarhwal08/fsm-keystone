package com.fsm.keystone.security;

import com.fsm.keystone.controller.*;
import com.fsm.keystone.dto.AuthResponse;
import com.fsm.keystone.dto.UserResponse;
import com.fsm.keystone.entity.*;
import com.fsm.keystone.enums.Role;
import com.fsm.keystone.fixtures.Personas;
import com.fsm.keystone.fixtures.TestJwtFactory;
import com.fsm.keystone.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Collections;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Parameterized MockMvc security slice that drives every endpoint-by-role combination
 * from {@code route-role-matrix.csv} against the real {@link SecurityFilterChain}.
 *
 * <p><b>Design:</b></p>
 * <ul>
 *   <li>Imports the production {@link SecurityConfig} and real {@link JwtAuthenticationFilter}
 *       so the actual filter chain runs without a database.</li>
 *   <li>Mocks all service and repository collaborators. No Flyway, no PostgreSQL.</li>
 *   <li>Parameterised test asserts status <em>class</em> (2xx / 403 / 401) so that
 *       mocked service responses do not make assertions brittle.</li>
 *   <li>CSRF is disabled in production {@link SecurityConfig}; mutating test requests
 *       deliberately omit CSRF tokens. A future CSRF enablement would break this test,
 *       making the change intentional and visible.</li>
 * </ul>
 *
 * <p><b>Bearer-token coverage</b> (at least one endpoint per controller) validates the
 * real {@link JwtAuthenticationFilter} path by using {@link TestJwtFactory} tokens and
 * stubbing the {@link UserDetailsService} lookup.</p>
 */
@Tag("slice")
@WebMvcTest({
        AuthController.class,
        CustomerController.class,
        DashboardController.class,
        PartController.class,
        PartUsageController.class,
        SiteController.class,
        TimeLogController.class,
        UserController.class,
        WorkOrderController.class
})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origin=http://localhost:5173",
        "app.jwt.secret=test-fixture-jwt-secret-key-must-be-at-least-64-chars-00000000",
        "app.jwt.expiration-ms=3600000"
})
class RouteRoleMatrixTest {

    @Autowired
    MockMvc mockMvc;

    // ── security collaborators ────────────────────────────────────────────────
    @MockBean
    AuthenticationProvider authenticationProvider;

    @MockBean
    UserDetailsService userDetailsService;

    // ── service collaborators ─────────────────────────────────────────────────
    @MockBean WorkOrderService workOrderService;
    @MockBean CustomerService  customerService;
    @MockBean PartService      partService;
    @MockBean SiteService      siteService;
    @MockBean UserService      userService;
    @MockBean DashboardService dashboardService;
    @MockBean AuthService      authService;

    private static final RouteRoleMatrixLoader LOADER = new RouteRoleMatrixLoader();

    @BeforeEach
    void stubServices() {
        // WorkOrderService
        WorkOrder stubWo = new WorkOrder();
        stubWo.setId(1L);
        when(workOrderService.createWorkOrder(any())).thenReturn(stubWo);
        when(workOrderService.getAllWorkOrders()).thenReturn(Collections.emptyList());
        when(workOrderService.getWorkOrderById(anyLong())).thenReturn(stubWo);
        when(workOrderService.assignTechnician(anyLong(), any())).thenReturn(stubWo);
        when(workOrderService.updateStatus(anyLong(), any())).thenReturn(stubWo);
        when(workOrderService.getWorkOrderHistory(anyLong())).thenReturn(Collections.emptyList());
        when(workOrderService.addPartUsage(anyLong(), any())).thenReturn(new PartUsage());
        when(workOrderService.addTimeLog(anyLong(), any())).thenReturn(new TimeLog());
        when(workOrderService.updateWorkOrder(anyLong(), any())).thenReturn(stubWo);

        // CustomerService
        Customer stubCustomer = new Customer();
        stubCustomer.setId(1L);
        when(customerService.createCustomer(any())).thenReturn(stubCustomer);
        when(customerService.getAllCustomers()).thenReturn(Collections.emptyList());
        when(customerService.getCustomerById(anyLong())).thenReturn(stubCustomer);
        when(customerService.updateCustomer(anyLong(), any())).thenReturn(stubCustomer);

        // PartService
        Part stubPart = new Part();
        stubPart.setId(1L);
        when(partService.createPart(any())).thenReturn(stubPart);
        when(partService.getAllParts()).thenReturn(Collections.emptyList());
        when(partService.getPartById(anyLong())).thenReturn(stubPart);
        when(partService.updatePart(anyLong(), any())).thenReturn(stubPart);

        // SiteService
        Site stubSite = new Site();
        stubSite.setId(1L);
        when(siteService.createSite(any())).thenReturn(stubSite);
        when(siteService.getAllSites()).thenReturn(Collections.emptyList());
        when(siteService.getSitesByCustomerId(anyLong())).thenReturn(Collections.emptyList());
        when(siteService.updateSite(anyLong(), any())).thenReturn(stubSite);

        // UserService
        UserResponse stubUserResponse = new UserResponse(
                1L, "Test User", "test@fixture.test", null, Role.MANAGER, true, null, null);
        when(userService.getAllUsers()).thenReturn(Collections.emptyList());
        when(userService.getAllTechnicians()).thenReturn(Collections.emptyList());
        when(userService.getMyProfile(any())).thenReturn(stubUserResponse);
        when(userService.updateUser(anyLong(), any())).thenReturn(stubUserResponse);

        // DashboardService
        when(dashboardService.getDashboardSummary()).thenReturn(Collections.emptyMap());

        // AuthService
        AuthResponse stubAuthResponse = new AuthResponse(
                "stub-token", 1L, "Test User", "test@fixture.test", Role.MANAGER, null, null);
        when(authService.signup(any())).thenReturn(stubAuthResponse);
        when(authService.login(any())).thenReturn(stubAuthResponse);
    }

    // ── parameterised matrix test ─────────────────────────────────────────────

    /**
     * Drives every matrix row × role combination (34 endpoints × 5 actors = 170 assertions).
     *
     * <p>Uses {@code SecurityMockMvcRequestPostProcessors.user()} for coarse role-based
     * identities; the JWT filter sees no {@code Authorization} header and chains through.
     * The security context populated by the post-processor is the authentication source.</p>
     *
     * <p>CSRF is disabled in {@link SecurityConfig}; mutating requests omit CSRF tokens
     * intentionally. A future CSRF enablement would break this test — that is by design.</p>
     */
    @ParameterizedTest(name = "[{index}] {0} {1} as {2} → expect {3}xx")
    @MethodSource("matrixArguments")
    void routeRoleMatrix(String method, String pathTemplate, String actor, int expectedStatus)
            throws Exception {
        RouteRoleMatrixRow row = LOADER.findRow(method, pathTemplate);
        String body = row.requiresBody() ? LOADER.loadBody(row.bodyFixtureKey()) : null;
        MockHttpServletRequestBuilder request = buildRequest(method, row.resolvedPath(), body, actor);

        mockMvc.perform(request)
                .andExpect(statusClass(expectedStatus));
    }

    static Stream<Arguments> matrixArguments() {
        return LOADER.stream().flatMap(row -> Stream.of(
                Arguments.of(row.method(), row.pathTemplate(), "ANONYMOUS",   row.expectedAnonymous()),
                Arguments.of(row.method(), row.pathTemplate(), "MANAGER",     row.expectedManager()),
                Arguments.of(row.method(), row.pathTemplate(), "DISPATCHER",  row.expectedDispatcher()),
                Arguments.of(row.method(), row.pathTemplate(), "TECHNICIAN",  row.expectedTechnician()),
                Arguments.of(row.method(), row.pathTemplate(), "CUSTOMER",    row.expectedCustomer())
        ));
    }

    // ── bearer-token coverage: one endpoint per controller ───────────────────

    /** AuthController: POST /api/auth/login with a real bearer token (MANAGER). */
    @Test
    void bearerToken_authController_loginReturns200() throws Exception {
        stubUserDetails(Personas.MANAGER);
        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOADER.loadBody("auth-login"))
                        .header("Authorization", TestJwtFactory.bearerHeaderFor(Personas.MANAGER)))
                .andExpect(status().is2xxSuccessful());
    }

    /** CustomerController: GET /api/customers with a real bearer token (MANAGER). */
    @Test
    void bearerToken_customerController_getAllReturns200ForManager() throws Exception {
        stubUserDetails(Personas.MANAGER);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/customers")
                        .header("Authorization", TestJwtFactory.bearerHeaderFor(Personas.MANAGER)))
                .andExpect(status().is2xxSuccessful());
    }

    /** DashboardController: GET /api/dashboard/summary with a MANAGER bearer token. */
    @Test
    void bearerToken_dashboardController_summaryReturns200ForManager() throws Exception {
        stubUserDetails(Personas.MANAGER);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/dashboard/summary")
                        .header("Authorization", TestJwtFactory.bearerHeaderFor(Personas.MANAGER)))
                .andExpect(status().is2xxSuccessful());
    }

    /** PartController: GET /api/parts with a real bearer token (TECHNICIAN). */
    @Test
    void bearerToken_partController_getAllReturns200ForTechnician() throws Exception {
        stubUserDetails(Personas.TECHNICIAN);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/parts")
                        .header("Authorization", TestJwtFactory.bearerHeaderFor(Personas.TECHNICIAN)))
                .andExpect(status().is2xxSuccessful());
    }

    /** PartUsageController: POST /api/part-usage with a DISPATCHER bearer token. */
    @Test
    void bearerToken_partUsageController_createReturns200ForDispatcher() throws Exception {
        stubUserDetails(Personas.DISPATCHER);
        mockMvc.perform(MockMvcRequestBuilders.post("/api/part-usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOADER.loadBody("part-usage-create"))
                        .header("Authorization", TestJwtFactory.bearerHeaderFor(Personas.DISPATCHER)))
                .andExpect(status().is2xxSuccessful());
    }

    /** SiteController: GET /api/sites with a DISPATCHER bearer token. */
    @Test
    void bearerToken_siteController_getAllReturns200ForDispatcher() throws Exception {
        stubUserDetails(Personas.DISPATCHER);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/sites")
                        .header("Authorization", TestJwtFactory.bearerHeaderFor(Personas.DISPATCHER)))
                .andExpect(status().is2xxSuccessful());
    }

    /** TimeLogController: POST /api/time-logs with a TECHNICIAN bearer token. */
    @Test
    void bearerToken_timeLogController_createReturns200ForTechnician() throws Exception {
        stubUserDetails(Personas.TECHNICIAN);
        mockMvc.perform(MockMvcRequestBuilders.post("/api/time-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOADER.loadBody("time-log-create"))
                        .header("Authorization", TestJwtFactory.bearerHeaderFor(Personas.TECHNICIAN)))
                .andExpect(status().is2xxSuccessful());
    }

    /** UserController: GET /api/users with a MANAGER bearer token. */
    @Test
    void bearerToken_userController_getAllReturns200ForManager() throws Exception {
        stubUserDetails(Personas.MANAGER);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/users")
                        .header("Authorization",
                                TestJwtFactory.bearerHeaderFor(Personas.MANAGER)))
                .andExpect(status().is2xxSuccessful());
    }

    /** WorkOrderController: GET /api/work-orders with a TECHNICIAN bearer token. */
    @Test
    void bearerToken_workOrderController_getAllReturns200ForTechnician() throws Exception {
        stubUserDetails(Personas.TECHNICIAN);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/work-orders")
                        .header("Authorization", TestJwtFactory.bearerHeaderFor(Personas.TECHNICIAN)))
                .andExpect(status().is2xxSuccessful());
    }

    // ── cross-check: matrix row count matches WO-010 endpoint inventory ──────

    /**
     * Fails the build if route-role-matrix.csv row count diverges from the WO-010
     * endpoint inventory baseline (EndpointAuthorizationInventoryTest.EXPECTED_ENDPOINT_COUNT).
     * A new controller endpoint without a corresponding matrix row triggers this guard.
     */
    @Test
    void matrixCoversAllInventoriedEndpoints() {
        // Keep in sync with EndpointAuthorizationInventoryTest.EXPECTED_ENDPOINT_COUNT
        int expectedEndpointCount = 35;
        int matrixCount = LOADER.rows().size();
        assertEquals(expectedEndpointCount, matrixCount,
                "route-role-matrix.csv has " + matrixCount + " rows but WO-010 inventory "
                + "expects " + expectedEndpointCount + " endpoints. "
                + "Add a new row to route-role-matrix.csv (or update the constant) when a "
                + "controller handler is added or removed.");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Stubs the UserDetailsService to return the given persona when the filter calls
     * {@code loadUserByUsername(persona.getEmail())}. Used for bearer-token coverage tests.
     */
    private void stubUserDetails(AppUser persona) {
        when(userDetailsService.loadUserByUsername(persona.getEmail())).thenReturn(persona);
    }

    private static MockHttpServletRequestBuilder buildRequest(
            String method, String resolvedPath, String body, String actor) {
        MockHttpServletRequestBuilder builder = switch (method.toUpperCase()) {
            case "GET"    -> MockMvcRequestBuilders.get(resolvedPath);
            case "POST"   -> MockMvcRequestBuilders.post(resolvedPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body != null ? body : "{}");
            case "PUT"    -> MockMvcRequestBuilders.put(resolvedPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body != null ? body : "{}");
            case "PATCH"  -> MockMvcRequestBuilders.patch(resolvedPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body != null ? body : "{}");
            case "DELETE" -> MockMvcRequestBuilders.delete(resolvedPath);
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        };

        if (!"ANONYMOUS".equalsIgnoreCase(actor)) {
            builder = builder.with(user("test@fixture.test").roles(actor));
        }
        return builder;
    }

    /**
     * Maps an expected status code to a status-class matcher.
     * Asserts authorization <em>outcome</em> rather than exact service-driven codes,
     * so that mocked service responses don't make the suite brittle.
     */
    private static ResultMatcher statusClass(int expected) {
        if (expected >= 200 && expected < 300) return status().is2xxSuccessful();
        if (expected == 403) return status().isForbidden();
        if (expected == 401) return status().isUnauthorized();
        return status().is(expected);
    }
}
