package com.fsm.keystone.security;

import com.fsm.keystone.controller.DashboardController;
import com.fsm.keystone.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.Collections;
import java.util.stream.Stream;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security baseline @WebMvcTest for {@link DashboardController}.
 *
 * <p>{@code GET /api/dashboard/summary} is under {@code anyRequest().authenticated()}
 * AND has {@code @PreAuthorize("hasAnyRole('MANAGER','DISPATCHER')")}. Anonymous,
 * TECHNICIAN and CUSTOMER actors must be blocked (HTTP 403).</p>
 */
@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.cors.allowed-origin=http://localhost:5173",
        "app.jwt.secret=test-placeholder-jwt-secret-key-must-be-at-least-64-chars-long-0000",
        "app.jwt.expiration-ms=3600000"
})
class DashboardControllerSecurityBaselineTest extends BaseControllerSecurityTest {

    @MockBean
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() throws Exception {
        setupJwtFilterPassthrough();
        when(dashboardService.getDashboardSummary()).thenReturn(Collections.emptyMap());
    }

    @ParameterizedTest(name = "[{index}] {0} {1} as {2} → HTTP {3}")
    @MethodSource("dashboardEndpointMatrix")
    void securityBaseline(String method, String path, String role, int expectedStatus)
            throws Exception {
        mockMvc.perform(buildRequest(method, path, role))
                .andExpect(status().is(expectedStatus));
    }

    static Stream<Arguments> dashboardEndpointMatrix() {
        return MatrixLoader.forController("DashboardController")
                .flatMap(row -> Stream.of(
                        Arguments.of(row.method(), row.path(), "ANONYMOUS",  row.expectedStatusAnon()),
                        Arguments.of(row.method(), row.path(), "MANAGER",    row.expectedStatusManager()),
                        Arguments.of(row.method(), row.path(), "DISPATCHER", row.expectedStatusDispatcher()),
                        Arguments.of(row.method(), row.path(), "TECHNICIAN", row.expectedStatusTechnician()),
                        Arguments.of(row.method(), row.path(), "CUSTOMER",   row.expectedStatusCustomer())
                ));
    }
}
